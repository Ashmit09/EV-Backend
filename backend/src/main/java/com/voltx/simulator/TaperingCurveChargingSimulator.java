package com.voltx.simulator;

import com.voltx.util.EnergyCalculator;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;

/**
 * DC fast-charging model. Real lithium-ion batteries take full power up to roughly 80%
 * and then the battery management system tapers the current to avoid overheating.
 * Here power falls linearly from 100% at 80% charge to 20% at full charge.
 * The run is integrated minute by minute.
 */
@Component
@Order(1)
public class TaperingCurveChargingSimulator implements ChargingSimulator {

    static final double DC_EFFICIENCY = 0.95;
    static final double TAPER_START_PERCENT = 80.0;
    static final double MIN_POWER_FRACTION = 0.20;

    @Override
    public boolean supports(ChargingContext context) {
        return context.directCurrent();
    }

    @Override
    public ChargingProgress simulate(ChargingContext context, Duration simulatedElapsed) {
        double capacity = context.batteryCapacityKwh().doubleValue();
        double maxPower = context.effectivePowerKw().doubleValue();
        double target = context.targetPercentage();
        double battery = context.initialPercentage();
        long elapsedMinutes = Math.max(0, simulatedElapsed.toMinutes());

        long minute = 0;
        while (minute < elapsedMinutes && battery < target) {
            double gain = powerAt(battery, maxPower) * DC_EFFICIENCY / 60.0 / capacity * 100.0;
            battery = Math.min(target, battery + gain);
            minute++;
        }
        boolean reached = battery >= target;
        battery = Math.round(battery * 100.0) / 100.0;

        BigDecimal energy = EnergyCalculator.energyConsumed(
                context.batteryCapacityKwh(), context.initialPercentage(), battery);
        BigDecimal currentPower = reached
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(powerAt(battery, maxPower)).setScale(2, RoundingMode.HALF_UP);
        return new ChargingProgress(battery, energy, minute, currentPower, reached);
    }

    double powerAt(double batteryPercent, double maxPower) {
        if (batteryPercent < TAPER_START_PERCENT) {
            return maxPower;
        }
        double progressThroughTaper = (batteryPercent - TAPER_START_PERCENT) / (100.0 - TAPER_START_PERCENT);
        double fraction = 1.0 - progressThroughTaper * (1.0 - MIN_POWER_FRACTION);
        return maxPower * Math.max(MIN_POWER_FRACTION, fraction);
    }

    @Override
    public String name() {
        return "DC tapering curve";
    }
}
