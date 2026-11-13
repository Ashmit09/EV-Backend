package com.voltx.util;

import com.voltx.entity.Vehicle;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;

/**
 * Energy formulas used by the simulator, the booking estimate and billing.
 * <p>
 * Compile-time polymorphism (method overloading): three {@code energyConsumed} methods share a
 * name but take different parameters; the compiler picks one from the argument types.
 */
public final class EnergyCalculator {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal MINUTES_PER_HOUR = BigDecimal.valueOf(60);

    private EnergyCalculator() {
        // utility class
    }

    /** Energy (kWh) = battery capacity × (final% − initial%) / 100. */
    public static BigDecimal energyConsumed(BigDecimal batteryCapacityKwh, double initialPercentage,
                                            double finalPercentage) {
        if (finalPercentage < initialPercentage) {
            throw new IllegalArgumentException("Final percentage cannot be lower than the initial percentage");
        }
        BigDecimal delta = BigDecimal.valueOf(finalPercentage - initialPercentage);
        return batteryCapacityKwh.multiply(delta).divide(HUNDRED, 3, RoundingMode.HALF_UP);
    }

    /** Overload: takes the capacity from the vehicle. */
    public static BigDecimal energyConsumed(Vehicle vehicle, double initialPercentage, double finalPercentage) {
        return energyConsumed(vehicle.getBatteryCapacityKwh(), initialPercentage, finalPercentage);
    }

    /** Overload: energy delivered at constant power for a duration (kWh = kW × h). */
    public static BigDecimal energyConsumed(BigDecimal powerKw, Duration duration) {
        BigDecimal minutes = BigDecimal.valueOf(duration.toMinutes());
        return powerKw.multiply(minutes).divide(MINUTES_PER_HOUR, 3, RoundingMode.HALF_UP);
    }

    /** How many battery percentage points a given amount of energy represents. */
    public static double percentageGain(BigDecimal batteryCapacityKwh, BigDecimal energyKwh) {
        return energyKwh.multiply(HUNDRED).divide(batteryCapacityKwh, 4, RoundingMode.HALF_UP).doubleValue();
    }
}
