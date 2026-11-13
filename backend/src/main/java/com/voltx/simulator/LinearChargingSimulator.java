package com.voltx.simulator;

import com.voltx.util.EnergyCalculator;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * AC charging model: the on-board charger draws constant power until the target is reached.
 * About 10% of grid energy is lost converting AC to DC, so 90% reaches the battery.
 */
@Component
@Order(2)
public class LinearChargingSimulator implements ChargingSimulator {

    static final double AC_EFFICIENCY = 0.90;

    @Override
    public boolean supports(ChargingContext context) {
        return !context.directCurrent();
    }

    @Override
    public ChargingProgress simulate(ChargingContext context, Duration simulatedElapsed) {
        double capacity = context.batteryCapacityKwh().doubleValue();
        double power = context.effectivePowerKw().doubleValue();
        double percentPerMinute = power * AC_EFFICIENCY / 60.0 / capacity * 100.0;
        double needed = context.targetPercentage() - context.initialPercentage();
        long minutesToTarget = (long) Math.ceil(needed / percentPerMinute);

        long minutes = Math.max(0, Math.min(simulatedElapsed.toMinutes(), minutesToTarget));
        boolean reached = minutes >= minutesToTarget;
        double battery = reached
                ? context.targetPercentage()
                : Math.min(context.targetPercentage(), context.initialPercentage() + percentPerMinute * minutes);
        battery = Math.round(battery * 100.0) / 100.0;

        BigDecimal energy = EnergyCalculator.energyConsumed(
                context.batteryCapacityKwh(), context.initialPercentage(), battery);
        BigDecimal currentPower = reached ? BigDecimal.ZERO : context.effectivePowerKw();
        return new ChargingProgress(battery, energy, minutes, currentPower, reached);
    }

    @Override
    public String name() {
        return "Linear AC";
    }
}
