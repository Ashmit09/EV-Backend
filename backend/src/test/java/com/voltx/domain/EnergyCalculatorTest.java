package com.voltx.domain;

import com.voltx.TestFixtures;
import com.voltx.util.EnergyCalculator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Compile-time polymorphism: three overloads of energyConsumed. */
class EnergyCalculatorTest {

    @Test
    void capacityTimesPercentageDelta() {
        // 40.5 kWh × (80 − 30) / 100 = 20.25 kWh
        assertThat(EnergyCalculator.energyConsumed(new BigDecimal("40.5"), 30, 80)).isEqualByComparingTo("20.250");
    }

    @Test
    void vehicleOverloadUsesVehicleCapacity() {
        var car = TestFixtures.nexon(TestFixtures.customer());
        assertThat(EnergyCalculator.energyConsumed(car, 30, 80)).isEqualByComparingTo("20.250");
    }

    @Test
    void powerAndDurationOverload() {
        assertThat(EnergyCalculator.energyConsumed(new BigDecimal("50"), Duration.ofMinutes(30))).isEqualByComparingTo("25.000");
    }

    @Test
    void finalBelowInitialIsRejected() {
        assertThatThrownBy(() -> EnergyCalculator.energyConsumed(new BigDecimal("40"), 60, 50))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
