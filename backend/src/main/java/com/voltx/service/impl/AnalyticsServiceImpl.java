package com.voltx.service.impl;

import com.voltx.dto.response.AdminOverviewResponse;
import com.voltx.dto.response.CustomerAnalyticsResponse;
import com.voltx.dto.response.EnergyAnalyticsResponse;
import com.voltx.dto.response.MonthlyPoint;
import com.voltx.dto.response.RevenueAnalyticsResponse;
import com.voltx.dto.response.SessionResponse;
import com.voltx.dto.response.StationUtilization;
import com.voltx.entity.ChargingSession;
import com.voltx.entity.ChargingUnit;
import com.voltx.entity.Invoice;
import com.voltx.entity.Payment;
import com.voltx.enums.ChargerStatus;
import com.voltx.enums.InvoiceStatus;
import com.voltx.enums.ReservationStatus;
import com.voltx.enums.Role;
import com.voltx.enums.SessionStatus;
import com.voltx.enums.StationStatus;
import com.voltx.mapper.SessionMapper;
import com.voltx.repository.ChargingSessionRepository;
import com.voltx.repository.ChargingUnitRepository;
import com.voltx.repository.InvoiceRepository;
import com.voltx.repository.PaymentRepository;
import com.voltx.repository.ReservationRepository;
import com.voltx.repository.StationRepository;
import com.voltx.repository.UserRepository;
import com.voltx.repository.VehicleRepository;
import com.voltx.service.AnalyticsService;
import com.voltx.util.MoneyUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * All dashboard numbers are computed here from database rows — nothing is hard-coded in the UI.
 * Aggregations use the Streams API ({@code groupingBy}, {@code reducing}, {@code summingLong}).
 */
@Service
@Transactional(readOnly = true)
public class AnalyticsServiceImpl implements AnalyticsService {

    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MMM yy");
    private static final int RECENT_SESSIONS = 5;
    private static final int UTILISATION_DAYS = 30;

    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final StationRepository stationRepository;
    private final ChargingUnitRepository unitRepository;
    private final ReservationRepository reservationRepository;
    private final ChargingSessionRepository sessionRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final Clock clock;

    public AnalyticsServiceImpl(UserRepository userRepository, VehicleRepository vehicleRepository,
                                StationRepository stationRepository, ChargingUnitRepository unitRepository,
                                ReservationRepository reservationRepository, ChargingSessionRepository sessionRepository,
                                InvoiceRepository invoiceRepository, PaymentRepository paymentRepository, Clock clock) {
        this.userRepository = userRepository;
        this.vehicleRepository = vehicleRepository;
        this.stationRepository = stationRepository;
        this.unitRepository = unitRepository;
        this.reservationRepository = reservationRepository;
        this.sessionRepository = sessionRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.clock = clock;
    }

    @Override
    public CustomerAnalyticsResponse customer(Long userId) {
        List<ChargingSession> completed = sessionRepository.findByUserIdWithDetails(userId).stream()
                .filter(session -> session.getStatus() == SessionStatus.COMPLETED)
                .toList();
        List<Invoice> invoices = invoiceRepository.findByUserIdWithDetails(userId);
        Map<Long, Long> invoiceIdBySession = invoices.stream()
                .collect(Collectors.toMap(invoice -> invoice.getSession().getId(), Invoice::getId));

        BigDecimal energy = sum(completed, ChargingSession::getEnergyConsumedKwh);
        BigDecimal spent = sum(invoices, Invoice::getTotalAmount);
        BigDecimal outstanding = sum(invoices.stream().filter(i -> !i.isPaid()).toList(), Invoice::getTotalAmount);
        double averageMinutes = completed.stream()
                .mapToInt(session -> session.getSimulatedDurationMinutes() == null ? 0 : session.getSimulatedDurationMinutes())
                .average()
                .orElse(0);

        List<SessionResponse> recent = completed.stream()
                .limit(RECENT_SESSIONS)
                .map(s -> SessionMapper.toResponse(s, null, null, invoiceIdBySession.get(s.getId()), false))
                .toList();

        return new CustomerAnalyticsResponse(completed.size(), energy, spent, outstanding,
                Math.round(averageMinutes * 10.0) / 10.0,
                reservationRepository.countByUserIdAndStatusIn(userId, List.of(ReservationStatus.CONFIRMED)),
                vehicleRepository.countByOwnerId(userId),
                monthlySeries(invoices, 6), recent);
    }

    @Override
    public AdminOverviewResponse overview() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<ChargingUnit> units = unitRepository.findAllWithStation();
        long available = units.stream()
                .filter(unit -> unit.getStation().isActive() && unit.getStatus() == ChargerStatus.AVAILABLE)
                .count();

        // Start from every status with 0 so the chart always shows all five states.
        Map<String, Long> distribution = new LinkedHashMap<>();
        Arrays.stream(ChargerStatus.values()).forEach(status -> distribution.put(status.name(), 0L));
        unitRepository.countGroupedByStatus()
                .forEach(row -> distribution.put(row.getStatus().name(), row.getTotal()));

        LocalDate today = now.toLocalDate();
        return new AdminOverviewResponse(
                userRepository.count(),
                userRepository.countByRole(Role.CUSTOMER),
                stationRepository.count(),
                stationRepository.countByStatus(StationStatus.ACTIVE),
                units.size(),
                available,
                sessionRepository.countByStatus(SessionStatus.IN_PROGRESS),
                reservationRepository.countByStartTimeBetween(today.atStartOfDay(), today.plusDays(1).atStartOfDay()),
                MoneyUtils.round(sessionRepository.sumEnergyByStatus(SessionStatus.COMPLETED)),
                MoneyUtils.round(invoiceRepository.sumTotalAmount()),
                MoneyUtils.round(invoiceRepository.sumTotalAmountByStatus(InvoiceStatus.PAID)),
                MoneyUtils.round(invoiceRepository.sumTotalAmountByStatus(InvoiceStatus.UNPAID)),
                distribution,
                stationUtilization(now));
    }

    @Override
    public RevenueAnalyticsResponse revenue(int months) {
        List<Invoice> invoices = invoiceRepository.findAllWithDetails();
        Map<String, BigDecimal> byPlan = groupSum(invoices, i -> i.getPricingPlan().getLabel(), Invoice::getTotalAmount);
        Map<String, BigDecimal> byCity = groupSum(invoices,
                i -> i.getSession().getChargingUnit().getStation().getCity(), Invoice::getTotalAmount);
        List<Payment> successful = paymentRepository.findAllWithInvoice().stream().filter(Payment::isSuccessful).toList();
        Map<String, BigDecimal> byMethod = groupSum(successful, p -> p.getMethod().name(), Payment::getAmount);
        return new RevenueAnalyticsResponse(monthlySeries(invoices, clampMonths(months)), byPlan, byMethod, byCity);
    }

    @Override
    public EnergyAnalyticsResponse energy(int months) {
        List<Invoice> invoices = invoiceRepository.findAllWithDetails();
        Map<String, BigDecimal> byConnector = groupSum(invoices,
                i -> i.getSession().getChargingUnit().getConnectorType().getLabel(), Invoice::getEnergyKwh);
        Map<String, BigDecimal> byVehicleType = groupSum(invoices,
                i -> i.getSession().getVehicle().getVehicleType().name(), Invoice::getEnergyKwh);
        Map<String, BigDecimal> byCity = groupSum(invoices,
                i -> i.getSession().getChargingUnit().getStation().getCity(), Invoice::getEnergyKwh);
        return new EnergyAnalyticsResponse(monthlySeries(invoices, clampMonths(months)), byConnector, byVehicleType, byCity);
    }

    // ------------------------------------------------------------------ helpers

    private List<StationUtilization> stationUtilization(LocalDateTime now) {
        LocalDateTime since = now.minusDays(UTILISATION_DAYS);
        Map<Long, List<Invoice>> recentByStation = invoiceRepository.findAllWithDetails().stream()
                .filter(invoice -> !invoice.getIssuedAt().isBefore(since))
                .collect(Collectors.groupingBy(invoice -> invoice.getSession().getChargingUnit().getStation().getId()));

        Predicate<ChargingUnit> busy = unit -> unit.getStatus() == ChargerStatus.CHARGING
                || unit.getStatus() == ChargerStatus.RESERVED;

        return stationRepository.search(null, null, null).stream()
                .map(station -> {
                    List<ChargingUnit> units = station.getChargingUnits();
                    long busyCount = units.stream().filter(busy).count();
                    List<Invoice> recent = recentByStation.getOrDefault(station.getId(), List.of());
                    return new StationUtilization(station.getId(), station.getName(), station.getCity(), units.size(),
                            busyCount, percentage(busyCount, units.size()), recent.size(),
                            sum(recent, Invoice::getEnergyKwh), sum(recent, Invoice::getTotalAmount));
                })
                .sorted((a, b) -> Long.compare(b.sessionsLast30Days(), a.sessionsLast30Days()))
                .toList();
    }

    /** Builds a gap-free monthly series: months without data appear with zeros. */
    private List<MonthlyPoint> monthlySeries(List<Invoice> invoices, int months) {
        YearMonth current = YearMonth.from(LocalDate.now(clock));
        Map<YearMonth, List<Invoice>> byMonth = invoices.stream()
                .collect(Collectors.groupingBy(invoice -> YearMonth.from(invoice.getIssuedAt()), TreeMap::new,
                        Collectors.toList()));
        return IntStream.rangeClosed(0, months - 1)
                .mapToObj(offset -> current.minusMonths(months - 1L - offset))
                .map(month -> {
                    List<Invoice> rows = byMonth.getOrDefault(month, List.of());
                    return new MonthlyPoint(month.format(MONTH_LABEL), sum(rows, Invoice::getEnergyKwh),
                            sum(rows, Invoice::getTotalAmount),
                            sum(rows.stream().filter(Invoice::isPaid).toList(), Invoice::getTotalAmount),
                            rows.size());
                })
                .toList();
    }

    /** Generic helper: works for any element type T and any BigDecimal-valued getter. */
    private static <T> BigDecimal sum(List<T> items, Function<T, BigDecimal> value) {
        return MoneyUtils.round(items.stream()
                .map(value)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private static <T> Map<String, BigDecimal> groupSum(List<T> items, Function<T, String> key,
                                                        Function<T, BigDecimal> value) {
        return items.stream().collect(Collectors.groupingBy(key, TreeMap::new,
                Collectors.reducing(BigDecimal.ZERO, value, BigDecimal::add)))
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> MoneyUtils.round(e.getValue()),
                        (a, b) -> a, LinkedHashMap::new));
    }

    private static double percentage(long part, long total) {
        return total == 0 ? 0 : BigDecimal.valueOf(part * 100.0 / total).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private static int clampMonths(int months) {
        return Math.max(1, Math.min(months, 24));
    }
}
