package com.voltx.service.impl;

import com.voltx.config.VoltXProperties;
import com.voltx.dto.request.ReservationRequest;
import com.voltx.dto.response.CostEstimateResponse;
import com.voltx.dto.response.ReservationResponse;
import com.voltx.entity.ChargingUnit;
import com.voltx.entity.Reservation;
import com.voltx.entity.User;
import com.voltx.entity.Vehicle;
import com.voltx.enums.AuditAction;
import com.voltx.enums.ChargerStatus;
import com.voltx.enums.PricingPlan;
import com.voltx.enums.ReservationStatus;
import com.voltx.exception.BookingConflictException;
import com.voltx.exception.ChargerNotFoundException;
import com.voltx.exception.ReservationNotFoundException;
import com.voltx.exception.UnauthorizedOperationException;
import com.voltx.exception.UserNotFoundException;
import com.voltx.exception.VehicleNotFoundException;
import com.voltx.mapper.ReservationMapper;
import com.voltx.repository.ChargingUnitRepository;
import com.voltx.repository.ReservationRepository;
import com.voltx.repository.UserRepository;
import com.voltx.repository.VehicleRepository;
import com.voltx.service.AuditService;
import com.voltx.service.NotificationService;
import com.voltx.service.ReservationService;
import com.voltx.simulator.ChargingContext;
import com.voltx.simulator.ChargingProgress;
import com.voltx.simulator.ChargingSimulationEngine;
import com.voltx.strategy.pricing.PriceBreakdown;
import com.voltx.strategy.pricing.PricingStrategy;
import com.voltx.util.MoneyUtils;
import com.voltx.util.StrategyRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Slot booking. The critical section is {@link #create}: it locks the charger row
 * (SELECT ... FOR UPDATE), then checks for overlaps and inserts, all in one transaction.
 * A second request for the same charger waits for the lock and then sees the first booking,
 * so double booking is impossible even under concurrent requests.
 */
@Service
public class ReservationServiceImpl implements ReservationService {

    private static final Logger log = LoggerFactory.getLogger(ReservationServiceImpl.class);
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("dd MMM, HH:mm");

    private final ReservationRepository reservationRepository;
    private final ChargingUnitRepository unitRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final BookingPolicy bookingPolicy;
    private final ChargingSimulationEngine simulationEngine;
    private final StrategyRegistry<PricingPlan, PricingStrategy> pricingStrategies;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final VoltXProperties properties;
    private final Clock clock;

    public ReservationServiceImpl(ReservationRepository reservationRepository, ChargingUnitRepository unitRepository,
                                  VehicleRepository vehicleRepository, UserRepository userRepository,
                                  BookingPolicy bookingPolicy, ChargingSimulationEngine simulationEngine,
                                  StrategyRegistry<PricingPlan, PricingStrategy> pricingStrategies,
                                  AuditService auditService, NotificationService notificationService,
                                  VoltXProperties properties, Clock clock) {
        this.reservationRepository = reservationRepository;
        this.unitRepository = unitRepository;
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
        this.bookingPolicy = bookingPolicy;
        this.simulationEngine = simulationEngine;
        this.pricingStrategies = pricingStrategies;
        this.auditService = auditService;
        this.notificationService = notificationService;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    @Transactional
    public ReservationResponse create(Long userId, ReservationRequest request) {
        LocalDateTime now = LocalDateTime.now(clock);
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        Vehicle vehicle = vehicleRepository.findByIdAndOwnerId(request.vehicleId(), userId)
                .orElseThrow(() -> new VehicleNotFoundException(request.vehicleId()));
        // Row lock: concurrent bookings of this charger queue up here.
        ChargingUnit unit = unitRepository.findByIdForUpdate(request.chargingUnitId())
                .orElseThrow(() -> new ChargerNotFoundException(request.chargingUnitId()));

        LocalDateTime start = request.startTime().truncatedTo(ChronoUnit.MINUTES);
        LocalDateTime end = start.plusMinutes(request.durationMinutes());
        bookingPolicy.validate(new BookingContext(vehicle, unit, start, end, request.durationMinutes(), now));

        if (reservationRepository.existsOverlappingOnUnit(unit.getId(), start, end, ReservationStatus.SLOT_HOLDING)) {
            throw new BookingConflictException("Charger " + unit.getChargerCode()
                    + " is already booked between " + start.format(WHEN) + " and " + end.toLocalTime());
        }
        if (reservationRepository.existsOverlappingForVehicle(vehicle.getId(), start, end, ReservationStatus.SLOT_HOLDING)) {
            throw new BookingConflictException(vehicle.getDisplayName() + " already has a booking that overlaps this time");
        }
        int maxActive = properties.booking().maxActiveReservations();
        if (reservationRepository.countByUserIdAndStatusIn(userId, ReservationStatus.SLOT_HOLDING) >= maxActive) {
            throw new BookingConflictException("You can hold at most " + maxActive + " upcoming reservations");
        }

        Reservation reservation = reservationRepository.save(new Reservation(user, vehicle, unit, start, end));
        holdChargerIfDue(reservation, unit, now);

        auditService.record(user.getEmail(), AuditAction.RESERVATION_CREATED, "Reservation", reservation.getId(),
                unit.getChargerCode() + " " + start.format(WHEN) + " for " + request.durationMinutes() + " min");
        notificationService.notify(user, "Booking confirmed", String.format("%s at %s is booked for %s (%d min).",
                unit.getChargerCode(), unit.getStation().getName(), start.format(WHEN), request.durationMinutes()));
        return toResponse(reservation, now, false);
    }

    @Override
    @Transactional(readOnly = true)
    public CostEstimateResponse estimate(Long userId, ReservationRequest request) {
        Vehicle vehicle = vehicleRepository.findByIdAndOwnerId(request.vehicleId(), userId)
                .orElseThrow(() -> new VehicleNotFoundException(request.vehicleId()));
        ChargingUnit unit = unitRepository.findById(request.chargingUnitId())
                .orElseThrow(() -> new ChargerNotFoundException(request.chargingUnitId()));
        vehicle.checkCompatibility(unit);

        int target = vehicle.defaultTargetPercentage(unit);
        double startBattery = vehicle.getCurrentBatteryPercentage();
        BigDecimal power = vehicle.effectiveChargingPower(unit.getMaxPowerKw());
        ChargingContext context = new ChargingContext(vehicle.getBatteryCapacityKwh(), startBattery,
                Math.max(target, startBattery), power, unit.isDcFastCharger());
        ChargingProgress progress = simulationEngine.simulate(context, Duration.ofMinutes(request.durationMinutes()));
        PriceBreakdown price = pricingStrategies.get(unit.getPricingPlan())
                .calculatePrice(progress.energyDeliveredKwh(), unit.getPricePerKwh(), request.startTime());
        BigDecimal tax = price.subtotal().multiply(properties.billing().gstRate()).setScale(2, RoundingMode.HALF_UP);

        return new CostEstimateResponse(power, startBattery, progress.batteryPercentage(), target,
                progress.energyDeliveredKwh(), price.plan(), price.energyCost(), price.adjustmentLabel(),
                price.adjustmentAmount(), price.subtotal(), tax, MoneyUtils.round(price.subtotal().add(tax)),
                simulationEngine.simulatorFor(context).name());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationResponse> myReservations(Long userId) {
        LocalDateTime now = LocalDateTime.now(clock);
        return reservationRepository.findByUserIdWithDetails(userId).stream()
                .map(r -> toResponse(r, now, false))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ReservationResponse get(Long userId, Long reservationId) {
        return toResponse(findOwned(userId, reservationId), LocalDateTime.now(clock), false);
    }

    @Override
    @Transactional
    public ReservationResponse cancel(Long userId, Long reservationId, String reason) {
        Reservation reservation = findOwned(userId, reservationId);
        LocalDateTime now = LocalDateTime.now(clock);
        cancelAndRelease(reservation, reason == null || reason.isBlank() ? "Cancelled by customer" : reason, now);
        auditService.record(reservation.getUser().getEmail(), AuditAction.RESERVATION_CANCELLED, "Reservation",
                reservationId, reservation.getCancellationReason());
        return toResponse(reservation, now, false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationResponse> listAll(ReservationStatus status) {
        LocalDateTime now = LocalDateTime.now(clock);
        return reservationRepository.findAllWithDetails(status).stream()
                .map(r -> toResponse(r, now, true))
                .toList();
    }

    @Override
    @Transactional
    public ReservationResponse cancelAsAdmin(String adminEmail, Long reservationId, String reason) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException(reservationId));
        LocalDateTime now = LocalDateTime.now(clock);
        cancelAndRelease(reservation, reason == null || reason.isBlank() ? "Cancelled by VoltX operations" : reason, now);
        auditService.record(adminEmail, AuditAction.RESERVATION_CANCELLED, "Reservation", reservationId,
                "Admin cancellation: " + reservation.getCancellationReason());
        notificationService.notify(reservation.getUser(), "Booking cancelled",
                "Your booking #" + reservationId + " was cancelled: " + reservation.getCancellationReason());
        return toResponse(reservation, now, true);
    }

    @Override
    @Transactional
    public int cancelUpcomingForCharger(Long chargerId, String reason, String actorEmail) {
        LocalDateTime now = LocalDateTime.now(clock);
        return cancelAll(reservationRepository.findByChargingUnitIdAndStatusAndEndTimeAfter(
                chargerId, ReservationStatus.CONFIRMED, now), reason, actorEmail, now);
    }

    @Override
    @Transactional
    public int cancelUpcomingForStation(Long stationId, String reason, String actorEmail) {
        LocalDateTime now = LocalDateTime.now(clock);
        return cancelAll(reservationRepository.findByChargingUnitStationIdAndStatusAndEndTimeAfter(
                stationId, ReservationStatus.CONFIRMED, now), reason, actorEmail, now);
    }

    @Override
    @Transactional
    public int activateDueReservations() {
        LocalDateTime now = LocalDateTime.now(clock);
        long window = properties.booking().startWindowMinutes();
        List<Reservation> due = reservationRepository.findByStatusAndStartTimeLessThanEqualAndEndTimeGreaterThan(
                ReservationStatus.CONFIRMED, now.plusMinutes(window), now);
        int held = 0;
        for (Reservation reservation : due) {
            ChargingUnit unit = reservation.getChargingUnit();
            if (unit.getStatus() == ChargerStatus.AVAILABLE && unit.getStation().isActive()) {
                unit.reserve();
                held++;
            }
        }
        return held;
    }

    @Override
    @Transactional
    public int expireMissedReservations() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<Reservation> missed = reservationRepository.findByStatusAndEndTimeLessThanEqual(
                ReservationStatus.CONFIRMED, now);
        missed.forEach(reservation -> {
            reservation.expire();
            releaseIfHeld(reservation.getChargingUnit(), reservation, now);
            auditService.record("system@voltx", AuditAction.RESERVATION_EXPIRED, "Reservation", reservation.getId(),
                    "No-show: slot ended without a charging session");
            notificationService.notify(reservation.getUser(), "Booking expired",
                    "Your booking #" + reservation.getId() + " expired because charging was not started.");
        });
        if (!missed.isEmpty()) {
            log.info("Expired {} missed reservations", missed.size());
        }
        return missed.size();
    }

    // ------------------------------------------------------------------ helpers

    private int cancelAll(List<Reservation> reservations, String reason, String actorEmail, LocalDateTime now) {
        reservations.forEach(reservation -> {
            cancelAndRelease(reservation, reason, now);
            auditService.record(actorEmail, AuditAction.RESERVATION_CANCELLED, "Reservation", reservation.getId(), reason);
            notificationService.notify(reservation.getUser(), "Booking cancelled",
                    "Your booking #" + reservation.getId() + " was cancelled: " + reason);
        });
        return reservations.size();
    }

    private void cancelAndRelease(Reservation reservation, String reason, LocalDateTime now) {
        reservation.cancel(reason, now);
        releaseIfHeld(reservation.getChargingUnit(), reservation, now);
    }

    /** If this reservation is the one currently holding the charger, free the charger. */
    private void releaseIfHeld(ChargingUnit unit, Reservation reservation, LocalDateTime now) {
        boolean slotIsCurrent = !now.isBefore(reservation.getStartTime().minusMinutes(
                properties.booking().startWindowMinutes()));
        if (unit.getStatus() == ChargerStatus.RESERVED && slotIsCurrent) {
            unit.releaseReservation();
        }
    }

    private void holdChargerIfDue(Reservation reservation, ChargingUnit unit, LocalDateTime now) {
        if (reservation.isWithinStartWindow(now, properties.booking().startWindowMinutes())
                && unit.getStatus() == ChargerStatus.AVAILABLE) {
            unit.reserve();
        }
    }

    private Reservation findOwned(Long userId, Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException(reservationId));
        if (!reservation.isOwnedBy(userId)) {
            throw new UnauthorizedOperationException("You can only access your own reservations");
        }
        return reservation;
    }

    private ReservationResponse toResponse(Reservation reservation, LocalDateTime now, boolean includeCustomer) {
        return ReservationMapper.toResponse(reservation, now, properties.booking().startWindowMinutes(), includeCustomer);
    }
}
