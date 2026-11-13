package com.voltx.config;

import com.voltx.entity.ChargingSession;
import com.voltx.entity.ChargingUnit;
import com.voltx.entity.ElectricBike;
import com.voltx.entity.ElectricCar;
import com.voltx.entity.ElectricScooter;
import com.voltx.entity.Invoice;
import com.voltx.entity.Payment;
import com.voltx.entity.Reservation;
import com.voltx.entity.User;
import com.voltx.entity.Vehicle;
import com.voltx.enums.ConnectorType;
import com.voltx.enums.PaymentMethod;
import com.voltx.enums.PaymentStatus;
import com.voltx.enums.PricingPlan;
import com.voltx.enums.Role;
import com.voltx.repository.ChargingSessionRepository;
import com.voltx.repository.ChargingUnitRepository;
import com.voltx.repository.InvoiceRepository;
import com.voltx.repository.PaymentRepository;
import com.voltx.repository.ReservationRepository;
import com.voltx.repository.UserRepository;
import com.voltx.repository.VehicleRepository;
import com.voltx.simulator.ChargingProgress;
import com.voltx.simulator.ChargingSimulationEngine;
import com.voltx.strategy.pricing.PriceBreakdown;
import com.voltx.strategy.pricing.PricingStrategy;
import com.voltx.util.EnergyCalculator;
import com.voltx.util.StrategyRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Creates demo accounts, vehicles and six months of completed charging history on an empty
 * database, so every dashboard shows real (database-backed) numbers from the first run.
 * History is produced with the same domain objects, simulator and pricing strategies as live
 * sessions — it is not hard-coded output. Disable with VOLTX_SEED_DEMO_DATA=false.
 */
@Component
@ConditionalOnProperty(prefix = "voltx.seed", name = "enabled", havingValue = "true")
public class DemoDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    private static final String CUSTOMER_PASSWORD = "Customer@123";
    private static final String ADMIN_PASSWORD = "Admin@123";
    private static final int HISTORY_DAYS = 175;

    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final ChargingUnitRepository unitRepository;
    private final ReservationRepository reservationRepository;
    private final ChargingSessionRepository sessionRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final PasswordEncoder passwordEncoder;
    private final ChargingSimulationEngine simulationEngine;
    private final StrategyRegistry<PricingPlan, PricingStrategy> pricingStrategies;
    private final VoltXProperties properties;
    private final Clock clock;
    private final Random random = new Random(2026);

    public DemoDataSeeder(UserRepository userRepository, VehicleRepository vehicleRepository,
                          ChargingUnitRepository unitRepository, ReservationRepository reservationRepository,
                          ChargingSessionRepository sessionRepository, InvoiceRepository invoiceRepository,
                          PaymentRepository paymentRepository, PasswordEncoder passwordEncoder,
                          ChargingSimulationEngine simulationEngine,
                          StrategyRegistry<PricingPlan, PricingStrategy> pricingStrategies,
                          VoltXProperties properties, Clock clock) {
        this.userRepository = userRepository;
        this.vehicleRepository = vehicleRepository;
        this.unitRepository = unitRepository;
        this.reservationRepository = reservationRepository;
        this.sessionRepository = sessionRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.passwordEncoder = passwordEncoder;
        this.simulationEngine = simulationEngine;
        this.pricingStrategies = pricingStrategies;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }
        log.info("Seeding VoltX demo data...");
        userRepository.save(new User("VoltX Administrator", "admin@voltx.in",
                passwordEncoder.encode(ADMIN_PASSWORD), "9000000001", Role.ADMIN));
        String customerHash = passwordEncoder.encode(CUSTOMER_PASSWORD);
        User aarav = userRepository.save(new User("Aarav Mehta", "aarav@voltx.in", customerHash, "9876543210", Role.CUSTOMER));
        User priya = userRepository.save(new User("Priya Sharma", "priya@voltx.in", customerHash, "9123456780", Role.CUSTOMER));
        User rohan = userRepository.save(new User("Rohan Iyer", "rohan@voltx.in", customerHash, null, Role.CUSTOMER));

        Vehicle nexon = vehicleRepository.save(new ElectricCar(aarav, "Tata", "Nexon EV Max", "KA01MJ4521",
                new BigDecimal("40.5"), new BigDecimal("50"), 34, Set.of(ConnectorType.CCS2, ConnectorType.TYPE_2)));
        Vehicle ather = vehicleRepository.save(new ElectricScooter(aarav, "Ather", "450X", "KA05HX7788",
                new BigDecimal("3.7"), new BigDecimal("3.3"), 58, Set.of(ConnectorType.TYPE_2)));
        Vehicle zs = vehicleRepository.save(new ElectricCar(priya, "MG", "ZS EV", "MH02FZ1209",
                new BigDecimal("50.3"), new BigDecimal("76"), 42, Set.of(ConnectorType.CCS2, ConnectorType.TYPE_2)));
        Vehicle leaf = vehicleRepository.save(new ElectricCar(rohan, "Nissan", "Leaf", "DL3CAF9090",
                new BigDecimal("40"), new BigDecimal("50"), 27, Set.of(ConnectorType.CHADEMO, ConnectorType.TYPE_2)));
        Vehicle f77 = vehicleRepository.save(new ElectricBike(rohan, "Ultraviolette", "F77", "TS09EV3344",
                new BigDecimal("10.3"), new BigDecimal("10"), 51, Set.of(ConnectorType.CCS2, ConnectorType.TYPE_2)));

        List<ChargingUnit> units = unitRepository.findAllWithStation().stream()
                .filter(unit -> unit.getStation().isActive())
                .toList();
        Map<Vehicle, Integer> sessionsPerVehicle = Map.of(nexon, 18, ather, 8, zs, 14, leaf, 9, f77, 7);
        int created = 0;
        for (Map.Entry<Vehicle, Integer> entry : sessionsPerVehicle.entrySet()) {
            List<ChargingUnit> compatible = units.stream()
                    .filter(unit -> entry.getKey().findCompatibilityIssue(unit).isEmpty())
                    .toList();
            for (int i = 0; i < entry.getValue(); i++) {
                createHistoricalSession(entry.getKey(), compatible.get(random.nextInt(compatible.size())));
                created++;
            }
        }
        log.info("Seeded 4 users, 5 vehicles and {} historical charging sessions", created);
        log.info("Demo logins - admin@voltx.in / {} and aarav@voltx.in / {}", ADMIN_PASSWORD, CUSTOMER_PASSWORD);
    }

    private void createHistoricalSession(Vehicle vehicle, ChargingUnit unit) {
        LocalDate day = LocalDate.now(clock).minusDays(1 + random.nextInt(HISTORY_DAYS));
        LocalTime time = LocalTime.of(7 + random.nextInt(14), 15 * random.nextInt(4));
        LocalDateTime start = LocalDateTime.of(day, time);
        int slotMinutes = 30 + 15 * random.nextInt(5);

        Reservation reservation = new Reservation(vehicle.getOwner(), vehicle, unit, start, start.plusMinutes(slotMinutes));
        reservation.backdate(start.minusHours(2 + random.nextInt(48)));
        reservation.activate();
        reservationRepository.save(reservation);

        double initial = 8 + random.nextInt(45);
        double target = Math.max(vehicle.defaultTargetPercentage(unit), initial + 5);
        BigDecimal power = vehicle.effectiveChargingPower(unit.getMaxPowerKw());
        ChargingSession session = new ChargingSession(reservation, start, initial, target, power);
        session.backdate(start);

        ChargingProgress progress = simulationEngine.simulate(ChargingSimulationEngine.contextOf(session),
                Duration.ofMinutes(slotMinutes));
        long minutes = Math.max(5, progress.simulatedMinutes());
        BigDecimal energy = EnergyCalculator.energyConsumed(vehicle, initial, progress.batteryPercentage());
        PriceBreakdown price = pricingStrategies.get(unit.getPricingPlan())
                .calculatePrice(energy, unit.getPricePerKwh(), start);
        LocalDateTime end = start.plusMinutes(minutes);
        session.complete(end, progress.batteryPercentage(), energy, (int) minutes, price.subtotal());
        reservation.complete();
        sessionRepository.save(session);

        Invoice invoice = invoiceRepository.save(Invoice.issue(session, price, properties.billing().gstRate(), end));
        boolean recent = day.isAfter(LocalDate.now(clock).minusDays(20));
        if (!recent || random.nextInt(3) > 0) {
            PaymentMethod method = invoice.getTotalAmount().compareTo(new BigDecimal("2000")) > 0
                    ? PaymentMethod.UPI
                    : PaymentMethod.values()[random.nextInt(PaymentMethod.values().length)];
            LocalDateTime paidAt = end.plusMinutes(3 + random.nextInt(60));
            paymentRepository.save(new Payment(invoice, method, PaymentStatus.SUCCESS,
                    method.name().substring(0, 3) + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(),
                    "Seeded historical payment", paidAt));
            invoice.markPaid(paidAt);
        }
    }
}
