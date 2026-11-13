package com.voltx.strategy;

import com.voltx.simulator.ChargingContext;
import com.voltx.simulator.ChargingProgress;
import com.voltx.simulator.ChargingSimulator;
import com.voltx.simulator.LinearChargingSimulator;
import com.voltx.simulator.TaperingCurveChargingSimulator;
import com.voltx.util.EnergyCalculator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class ChargingSimulatorTest {

    private final ChargingSimulator linear = new LinearChargingSimulator();
    private final ChargingSimulator curve = new TaperingCurveChargingSimulator();

    @Test
    void eachSimulatorSupportsItsCurrentType() {
        ChargingContext ac = new ChargingContext(new BigDecimal("40"), 20, 100, new BigDecimal("7.4"), false);
        ChargingContext dc = new ChargingContext(new BigDecimal("40"), 20, 80, new BigDecimal("50"), true);
        assertThat(linear.supports(ac)).isTrue();
        assertThat(linear.supports(dc)).isFalse();
        assertThat(curve.supports(dc)).isTrue();
    }

    @Test
    void linearAcProgressIsProportionalToTime() {
        ChargingContext ac = new ChargingContext(new BigDecimal("40"), 20, 100, new BigDecimal("10"), false);
        ChargingProgress oneHour = linear.simulate(ac, Duration.ofMinutes(60));
        // 10 kW × 0.9 efficiency × 1 h = 9 kWh = 22.5% of 40 kWh
        assertThat(oneHour.batteryPercentage()).isEqualTo(42.5);
        assertThat(oneHour.energyDeliveredKwh()).isEqualByComparingTo("9.000");
        assertThat(oneHour.targetReached()).isFalse();
    }

    @Test
    void progressStopsAtTarget() {
        ChargingContext dc = new ChargingContext(new BigDecimal("40"), 20, 80, new BigDecimal("50"), true);
        ChargingProgress progress = curve.simulate(dc, Duration.ofHours(5));
        assertThat(progress.batteryPercentage()).isEqualTo(80.0);
        assertThat(progress.targetReached()).isTrue();
        assertThat(progress.currentPowerKw()).isEqualByComparingTo("0");
        assertThat(progress.energyDeliveredKwh())
                .isEqualByComparingTo(EnergyCalculator.energyConsumed(new BigDecimal("40"), 20, 80));
    }

    @Test
    void dcChargingSlowsDownAbove80Percent() {
        ChargingContext low = new ChargingContext(new BigDecimal("40"), 40, 100, new BigDecimal("50"), true);
        ChargingContext high = new ChargingContext(new BigDecimal("40"), 80, 100, new BigDecimal("50"), true);
        double gainLow = curve.simulate(low, Duration.ofMinutes(10)).batteryPercentage() - 40;
        double gainHigh = curve.simulate(high, Duration.ofMinutes(10)).batteryPercentage() - 80;
        assertThat(gainHigh).isLessThan(gainLow);
    }
}
