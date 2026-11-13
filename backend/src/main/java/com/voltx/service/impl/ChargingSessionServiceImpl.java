package com.voltx.service.impl;

import com.voltx.config.VoltXProperties;
import com.voltx.dto.request.StartSessionRequest;
import com.voltx.dto.response.SessionResponse;
import com.voltx.entity.ChargingSession;
import com.voltx.entity.ChargingUnit;
import com.voltx.entity.Invoice;
import com.voltx.entity.Reservation;
import com.voltx.entity.Vehicle;
import com.voltx.enums.AuditAction;
import com.voltx.enums.ChargerStatus;
import com.voltx.enums.PricingPlan;
import com.voltx.enums.ReservationStatus;
import com.voltx.enums.SessionStatus;
import com.voltx.exception.ChargerUnavailableException;
import com.voltx.exception.InvalidSessionException;
import com.voltx.exception.ReservationNotFoundException;
import com.voltx.exception.SessionNotFoundException;
import com.voltx.exception.UnauthorizedOperationException;
import com.voltx.mapper.SessionMapper;
import com.voltx.repository.ChargingSessionRepository;
import com.voltx.repository.ChargingUnitRepository;
import com.voltx.repository.InvoiceRepository;
import com.voltx.repository.ReservationRepository;
import com.voltx.service.AuditService;
import com.voltx.service.BillingService;
import com.voltx.service.ChargingSessionService;
import com.voltx.service.NotificationService;
import com.voltx.simulator.ChargingProgress;
import com.voltx.simulator.ChargingSimulationEngine;
import com.voltx.strategy.pricing.PriceBreakdown;
import com.voltx.strategy.pricing.PricingStrategy;
import com.voltx.util.EnergyCalculator;
import com.voltx.util.StrategyRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Charging session business rules. It delegates the physics to {@link ChargingSimulationEngine}
 * and the money to a {@link PricingStrategy}; this class only coordinates state changes
 * (charger, reservation, vehicle, session, invoice) inside one transaction.
 */
@Service
public class ChargingSessionServiceImpl implements ChargingSessionService {

    private static final Logger log = LoggerFactory.getLogger(ChargingSessionServiceImpl.class);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final ChargingSessionRepository sessionRepository;
    private final ReservationRepository reservationRepository;
    private final ChargingUnitRepository unitRepository;
    private final InvoiceRepository invoiceRepository;
    private final ChargingSimulationEngine simulationEngine;
    private final StrategyRegistry<PricingPlan, PricingStrategy> pricingStrategies;
    private final BillingService billingService;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final VoltXProperties properties;
    private final Clock clock;

    public ChargingSessionServiceImpl(ChargingSessionRepository sessionRepository,
                                      ReservationRepository reservationRepository,
                                      ChargingUnitRepository unitRepository, InvoiceRepository invoiceRepository,
                                      ChargingSimulationEngine simulationEngine,
                                      StrategyRegistry<PricingPlan, PricingStrategy> pricingStrategies,
                                      BillingService billingService, AuditService auditService,
                                      NotificationService notificationService, VoltXProperties properties,
                                      Clock clock) {
        this.sessionRepository = sessionRepository;
        this.reservationRepository = reservationRepository;
        this.unitRepository = unitRepository;
        this.invoiceRepository = invoiceRepository;
        this.simulationEngine = simulationEngine;
        this.pricingStrategies = pricingStrategies;
        this.billingService = billingService;
        this.auditService = auditService;
        this.notificationService = notificationService;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    @Transactional
    public SessionResponse start(Long userId, StartSessionRequest request) {
        LocalDateTime now = LocalDateTime.now(clock);
        Reservation reservation = reservationRepository.findById(request.reservationId())
                .orElseThrow(() -> new ReservationNotFoundException(request.reservationId()));
        if (!reservation.isOwnedBy(userId)) {
            throw new UnauthorizedOperationException("You can only start charging on your own reservations");
        }
        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new InvalidSessionException("Reservation #" + reservation.getId() + " is " + reservation.getStatus());
        }
        long window = properties.booking().startWindowMinutes();
        if (!reservation.isWithinStartWindow(now, window)) {
            throw new InvalidSessionException(String.format(
                    "Charging for this booking can start between %s and %s",
                    reservation.getStartTime().minusMinutes(window).format(TIME),
                    reservation.getEndTime().format(TIME)));
        }
        Vehicle vehicle = reservation.getVehicle();
        if (sessionRepository.existsByVehicleIdAndStatus(vehicle.getId(), SessionStatus.IN_PROGRESS)) {
            throw new InvalidSessionException(vehicle.getDisplayName() + " is already charging");
        }

        ChargingUnit unit = unitRepository.findByIdForUpdate(reservation.getChargingUnit().getId())
                .orElseThrow(() -> new ChargerUnavailableException("Charger no longer exists"));
        vehicle.checkCompatibility(unit); // the admin may have changed the connector since booking
        if (unit.getStatus() == ChargerStatus.AVAILABLE) {
            unit.reserve();
        }
        unit.startCharging(); // RESERVED → CHARGING, or InvalidChargingStateException

        double initial = vehicle.getCurrentBatteryPercentage();
        double target = request.targetBatteryPercentage() != null
                ? request.targetBatteryPercentage()
                : Math.max(vehicle.defaultTargetPercentage(unit), Math.min(100, initial + 1));
        BigDecimal power = vehicle.effectiveChargingPower(unit.getMaxPowerKw());

        reservation.activate();
        ChargingSession session = sessionRepository.save(new ChargingSession(reservation, now, initial, target, power));

        auditService.record(reservation.getUser().getEmail(), AuditAction.SESSION_STARTED, "ChargingSession",
                session.getId(), String.format("%s on %s, %.0f%% → %.0f%% at %s kW", vehicle.getDisplayName(),
                        unit.getChargerCode(), initial, target, power.stripTrailingZeros().toPlainString()));
        notificationService.notify(reservation.getUser(), "Charging started",
                vehicle.getDisplayName() + " started charging on " + unit.getChargerCode() + ".");
        return live(session, now, false);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SessionResponse> active(Long userId) {
        LocalDateTime now = LocalDateTime.now(clock);
        return sessionRepository.findFirstByUserIdAndStatusOrderByStartTimeDesc(userId, SessionStatus.IN_PROGRESS)
                .map(session -> live(session, now, false));
    }

    @Override
    @Transactional(readOnly = true)
    public SessionResponse get(Long userId, Long sessionId) {
        ChargingSession session = findOwned(userId, sessionId);
        return session.isActive() ? live(session, LocalDateTime.now(clock), false) : completed(session, false);
    }

    @Override
    @Transactional
    public SessionResponse stop(Long userId, Long sessionId) {
        ChargingSession session = findOwned(userId, sessionId);
        finish(session, LocalDateTime.now(clock), session.getUser().getEmail());
        return completed(session, false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SessionResponse> history(Long userId) {
        LocalDateTime now = LocalDateTime.now(clock);
        return sessionRepository.findByUserIdWithDetails(userId).stream()
                .map(s -> s.isActive() ? live(s, now, false) : completed(s, false))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SessionResponse> listActive() {
        LocalDateTime now = LocalDateTime.now(clock);
        return sessionRepository.findByStatusWithDetails(SessionStatus.IN_PROGRESS).stream()
                .map(session -> live(session, now, true))
                .toList();
    }

    @Override
    @Transactional
    public SessionResponse stopAsAdmin(String adminEmail, Long sessionId) {
        ChargingSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException(sessionId));
        finish(session, LocalDateTime.now(clock), adminEmail);
        notificationService.notify(session.getUser(), "Charging stopped by operator",
                "Session #" + sessionId + " was stopped by VoltX operations. Your invoice is ready.");
        return completed(session, true);
    }

    @Override
    @Transactional
    public int autoCompleteSessions() {
        LocalDateTime now = LocalDateTime.now(clock);
        int finished = 0;
        for (ChargingSession session : sessionRepository.findByStatus(SessionStatus.IN_PROGRESS)) {
            ChargingProgress progress = simulationEngine.progressOf(session, now);
            boolean slotEnded = session.getReservation() != null && !now.isBefore(session.getReservation().getEndTime());
            if (progress.targetReached() || slotEnded) {
                finish(session, now, "system@voltx");
                finished++;
            }
        }
        if (finished > 0) {
            log.info("Auto-completed {} charging sessions", finished);
        }
        return finished;
    }

    // ------------------------------------------------------------------ core completion logic

    /**
     * Completes a session in one transaction:
     * simulate final state → energy (capacity × Δ% / 100) → price via strategy →
     * close session → free charger → update vehicle battery → complete reservation → issue invoice.
     */
    private void finish(ChargingSession session, LocalDateTime now, String actorEmail) {
        if (!session.isActive()) {
            throw new InvalidSessionException("Charging session #" + session.getId() + " has already been stopped");
        }
        ChargingProgress progress = simulationEngine.progressOf(session, now);
        Vehicle vehicle = session.getVehicle();
        double finalBattery = progress.batteryPercentage();
        BigDecimal energy = EnergyCalculator.energyConsumed(vehicle, session.getInitialBatteryPercentage(), finalBattery);
        PriceBreakdown price = pricingStrategies.get(session.getPricingPlan())
                .calculatePrice(energy, session.getPricePerKwh(), session.getStartTime());

        session.complete(now, finalBattery, energy, (int) progress.simulatedMinutes(), price.subtotal());

        ChargingUnit unit = unitRepository.findByIdForUpdate(session.getChargingUnit().getId())
                .orElseThrow(() -> new ChargerUnavailableException("Charger no longer exists"));
        unit.completeCharging();
        vehicle.updateBatteryLevel((int) Math.floor(finalBattery));
        if (session.getReservation() != null) {
            session.getReservation().complete();
        }
        Invoice invoice = billingService.generateInvoice(session, price);

        auditService.record(actorEmail, AuditAction.SESSION_COMPLETED, "ChargingSession", session.getId(),
                String.format("%.1f%% → %.1f%%, %s kWh, ₹%s", session.getInitialBatteryPercentage(), finalBattery,
                        energy.toPlainString(), price.subtotal().toPlainString()));
        notificationService.notify(session.getUser(), "Charging complete", String.format(
                "%s charged to %.0f%% (%s kWh). Invoice %s: ₹%s.", vehicle.getDisplayName(), finalBattery,
                energy.stripTrailingZeros().toPlainString(), invoice.getInvoiceNumber(),
                invoice.getTotalAmount().toPlainString()));
    }

    private SessionResponse live(ChargingSession session, LocalDateTime now, boolean includeCustomer) {
        ChargingProgress progress = simulationEngine.progressOf(session, now);
        BigDecimal cost = pricingStrategies.get(session.getPricingPlan())
                .calculatePrice(progress.energyDeliveredKwh(), session.getPricePerKwh(), session.getStartTime())
                .subtotal();
        return SessionMapper.toResponse(session, progress, cost, null, includeCustomer);
    }

    private SessionResponse completed(ChargingSession session, boolean includeCustomer) {
        Long invoiceId = invoiceRepository.findBySessionId(session.getId()).map(Invoice::getId).orElse(null);
        return SessionMapper.toResponse(session, null, null, invoiceId, includeCustomer);
    }

    private ChargingSession findOwned(Long userId, Long sessionId) {
        ChargingSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException(sessionId));
        if (!session.isOwnedBy(userId)) {
            throw new UnauthorizedOperationException("You can only access your own charging sessions");
        }
        return session;
    }
}
