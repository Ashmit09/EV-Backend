package com.voltx.simulator;

import java.math.BigDecimal;

/**
 * Everything a simulator needs to know about one charging run.
 *
 * @param effectivePowerKw min(charger power, vehicle max power)
 * @param directCurrent    true for DC fast charging (CCS2, CHAdeMO, GB/T)
 */
public record ChargingContext(
        BigDecimal batteryCapacityKwh,
        double initialPercentage,
        double targetPercentage,
        BigDecimal effectivePowerKw,
        boolean directCurrent) {
}
