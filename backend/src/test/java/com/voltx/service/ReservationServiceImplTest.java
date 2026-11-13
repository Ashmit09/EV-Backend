package com.voltx.service;

import com.voltx.TestFixtures;
import com.voltx.config.VoltXProperties;
import com.voltx.dto.request.ReservationRequest;
import com.voltx.entity.ChargingStation;
import com.voltx.entity.ChargingUnit;
import com.voltx.entity.Reservation;
import com.voltx.entity.User;
import com.voltx.entity.Vehicle;
import com.voltx.enums.ChargerStatus;
import com.voltx.enums.ConnectorType;
import com.voltx.enums.PricingPlan;
import com.voltx.enums.ReservationStatus;
import com.voltx.exception.BookingConflictException;
import com.voltx.exception.ChargerUnavailableException;
import com.voltx.exception.IncompatibleConnectorException;
import com.voltx.exception.InvalidBookingException;
import com.voltx.exception.UnauthorizedOperationException;
import com.voltx.repository.ChargingUnitRepository;
import com.voltx.repository.ReservationRepository;
import com.voltx.repository.UserRepository;
import com.voltx.repository.VehicleRepository;
import com.voltx.service.impl.BookingPolicy;
import com.voltx.service.impl.ReservationServiceImpl;
import com.voltx.simulator.ChargingSimulationEngine;
import com.voltx.simulator.LinearChargingSimulator;
import com.voltx.simulator.TaperingCurveChargingSimulator;
import com.voltx.strategy.pricing.PeakHourPricingStrategy;
import com.voltx.strategy.pricing.PricingStrategy;
import com.voltx.strategy.pricing.PromotionalPricingStrategy;
import com.voltx.strategy.pricing.StandardPricingStrategy;
import com.voltx.util.StrategyRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Unit test of the booking rules with mocked repositories (Mockito) and a frozen clock. */
@ExtendWith(MockitoExtension.class)
class ReservationServiceImplTest {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 13, 10, 0);

    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private ChargingUnitRepository unitRepository;
    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AuditService auditService;
    @Mock
    private NotificationService notificationService;

    private ReservationServiceImpl service;
    private User user;
    private Vehicle car;
    private ChargingStation station;
    private ChargingUnit dcUnit;

    @BeforeEach
    void setUp() {
        VoltXProperties properties = new VoltXProperties("Asia/Kolkata", new VoltXProperties.Jwt("", 60),
                new VoltXProperties.Cors(List.of()), new VoltXProperties.Simulation(60),
                new VoltXProperties.Booking(15, 240, 14, 15, 3), new VoltXProperties.Billing(new BigDecimal("0.18")),
                new VoltXProperties.Seed(false));
        Clock clock = Clock.fixed(NOW.atZone(IST).toInstant(), IST);
        StrategyRegistry<PricingPlan, PricingStrategy> pricing = new StrategyRegistry<>(PricingPlan.class,
                List.of(new StandardPricingStrategy(), new PeakHourPricingStrategy(), new PromotionalPricingStrategy()),
                PricingStrategy::plan, "pricing strategy");
        ChargingSimulationEngine engine = new ChargingSimulationEngine(
                List.of(new TaperingCurveChargingSimulator(), new LinearChargingSimulator()), properties);
        service = new ReservationServiceImpl(reservationRepository, unitRepository, vehicleRepository, userRepository,
                new BookingPolicy(properties), engine, pricing, auditService, notificationService, properties, clock);

        user = TestFixtures.customer();
        ReflectionTestUtils.setField(user, "id", 7L);
        car = TestFixtures.nexon(user);
        station = TestFixtures.station24h();
        dcUnit = TestFixtures.unit(station, "VX-DC", ConnectorType.CCS2, "60", PricingPlan.STANDARD);

        lenient().when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        lenient().when(vehicleRepository.findByIdAndOwnerId(1L, 7L)).thenReturn(Optional.of(car));
        lenient().when(unitRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(dcUnit));
        lenient().when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private ReservationRequest request(LocalDateTime start, int minutes) {
        return new ReservationRequest(1L, 2L, start, minutes);
    }

    @Test
    void bookingStartingNowHoldsTheCharger() {
        var response = service.create(7L, request(NOW, 60));
        assertThat(response.status()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(dcUnit.getStatus()).isEqualTo(ChargerStatus.RESERVED);
        verify(auditService).record(any(), any(), any(), any(), any());
    }

    @Test
    void futureBookingDoesNotHoldTheChargerYet() {
        service.create(7L, request(NOW.plusHours(3), 60));
        assertThat(dcUnit.getStatus()).isEqualTo(ChargerStatus.AVAILABLE);
    }

    @Test
    void overlappingBookingIsRejected() {
        when(reservationRepository.existsOverlappingOnUnit(any(), any(), any(), any())).thenReturn(true);
        assertThatThrownBy(() -> service.create(7L, request(NOW.plusHours(1), 60)))
                .isInstanceOf(BookingConflictException.class)
                .hasMessageContaining("already booked");
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void vehicleDoubleBookingIsRejected() {
        when(reservationRepository.existsOverlappingForVehicle(any(), any(), any(), any())).thenReturn(true);
        assertThatThrownBy(() -> service.create(7L, request(NOW.plusHours(1), 60)))
                .isInstanceOf(BookingConflictException.class);
    }

    @Test
    void tooManyActiveReservationsIsRejected() {
        when(reservationRepository.countByUserIdAndStatusIn(anyLong(), any())).thenReturn(3L);
        assertThatThrownBy(() -> service.create(7L, request(NOW.plusHours(1), 60)))
                .isInstanceOf(BookingConflictException.class)
                .hasMessageContaining("at most 3");
    }

    @Test
    void invalidDurationsAndPastStartsAreRejected() {
        assertThatThrownBy(() -> service.create(7L, request(NOW.plusHours(1), 10))).isInstanceOf(InvalidBookingException.class);
        assertThatThrownBy(() -> service.create(7L, request(NOW.plusHours(1), 50))).isInstanceOf(InvalidBookingException.class);
        assertThatThrownBy(() -> service.create(7L, request(NOW.minusHours(1), 60))).isInstanceOf(InvalidBookingException.class);
        assertThatThrownBy(() -> service.create(7L, request(NOW.plusDays(20), 60))).isInstanceOf(InvalidBookingException.class);
    }

    @Test
    void operatingHoursAreEnforced() {
        ChargingUnit dayUnit = TestFixtures.unit(TestFixtures.stationOpen(8, 20), "VX-DAY", ConnectorType.CCS2, "60",
                PricingPlan.STANDARD);
        when(unitRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(dayUnit));
        assertThatThrownBy(() -> service.create(7L, request(NOW.withHour(19).withMinute(30), 60)))
                .isInstanceOf(InvalidBookingException.class)
                .hasMessageContaining("08:00–20:00");
    }

    @Test
    void unavailableChargerIsRejected() {
        dcUnit.markUnderMaintenance();
        assertThatThrownBy(() -> service.create(7L, request(NOW.plusHours(1), 60)))
                .isInstanceOf(ChargerUnavailableException.class);
    }

    @Test
    void incompatibleConnectorIsRejected() {
        ChargingUnit chademo = TestFixtures.unit(station, "VX-CHA", ConnectorType.CHADEMO, "50", PricingPlan.STANDARD);
        when(unitRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(chademo));
        assertThatThrownBy(() -> service.create(7L, request(NOW.plusHours(1), 60)))
                .isInstanceOf(IncompatibleConnectorException.class);
    }

    @Test
    void cancellingAHeldReservationReleasesTheCharger() {
        Reservation reservation = new Reservation(user, car, dcUnit, NOW, NOW.plusHours(1));
        dcUnit.reserve();
        when(reservationRepository.findById(5L)).thenReturn(Optional.of(reservation));

        var response = service.cancel(7L, 5L, null);

        assertThat(response.status()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(dcUnit.getStatus()).isEqualTo(ChargerStatus.AVAILABLE);
    }

    @Test
    void customersCannotCancelOtherPeoplesReservations() {
        Reservation reservation = new Reservation(user, car, dcUnit, NOW, NOW.plusHours(1));
        when(reservationRepository.findById(5L)).thenReturn(Optional.of(reservation));
        assertThatThrownBy(() -> service.cancel(99L, 5L, null)).isInstanceOf(UnauthorizedOperationException.class);
    }
}
