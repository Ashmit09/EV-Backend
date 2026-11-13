package com.voltx.domain;

import com.voltx.TestFixtures;
import com.voltx.entity.ChargingStation;
import com.voltx.entity.ChargingUnit;
import com.voltx.enums.ChargerStatus;
import com.voltx.enums.ConnectorType;
import com.voltx.enums.PricingPlan;
import com.voltx.exception.InvalidChargingStateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Encapsulation: the charger status can only change through validated transitions. */
class ChargingUnitStateMachineTest {

    private ChargingStation station;
    private ChargingUnit unit;

    @BeforeEach
    void setUp() {
        station = TestFixtures.station24h();
        unit = TestFixtures.unit(station, "VX-T-01", ConnectorType.CCS2, "60", PricingPlan.STANDARD);
    }

    @Test
    @DisplayName("AVAILABLE → RESERVED → CHARGING → AVAILABLE is the normal lifecycle")
    void happyPath() {
        unit.reserve();
        assertThat(unit.getStatus()).isEqualTo(ChargerStatus.RESERVED);
        unit.startCharging();
        assertThat(unit.getStatus()).isEqualTo(ChargerStatus.CHARGING);
        unit.completeCharging();
        assertThat(unit.getStatus()).isEqualTo(ChargerStatus.AVAILABLE);
    }

    @Test
    void cannotStartChargingWithoutReservation() {
        assertThatThrownBy(unit::startCharging)
                .isInstanceOf(InvalidChargingStateException.class)
                .hasMessageContaining("AVAILABLE to CHARGING");
    }

    @Test
    void cannotReserveTwice() {
        unit.reserve();
        assertThatThrownBy(unit::reserve).isInstanceOf(InvalidChargingStateException.class);
    }

    @Test
    void cannotTakeOutOfServiceWhileCharging() {
        unit.reserve();
        unit.startCharging();
        assertThatThrownBy(unit::markOutOfService).isInstanceOf(InvalidChargingStateException.class);
    }

    @Test
    void releaseOnlyWorksWhenReserved() {
        assertThatThrownBy(unit::releaseReservation).isInstanceOf(InvalidChargingStateException.class);
        unit.reserve();
        unit.releaseReservation();
        assertThat(unit.getStatus()).isEqualTo(ChargerStatus.AVAILABLE);
    }

    @Test
    void maintenanceAndRestore() {
        unit.markUnderMaintenance();
        assertThat(unit.acceptsBookings()).isFalse();
        unit.markOutOfService();
        unit.restoreService();
        assertThat(unit.getStatus()).isEqualTo(ChargerStatus.AVAILABLE);
        assertThatThrownBy(unit::restoreService).isInstanceOf(InvalidChargingStateException.class);
    }

    @Test
    void disabledStationBlocksReservation() {
        station.disable();
        assertThatThrownBy(unit::reserve).isInstanceOf(InvalidChargingStateException.class);
        assertThat(unit.acceptsBookings()).isFalse();
    }
}
