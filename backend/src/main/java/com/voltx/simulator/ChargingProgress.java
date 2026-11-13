package com.voltx.simulator;

import java.math.BigDecimal;

/** Snapshot of a simulated charging run at a point in time. */
public record ChargingProgress(
        double batteryPercentage,
        BigDecimal energyDeliveredKwh,
        long simulatedMinutes,
        BigDecimal currentPowerKw,
        boolean targetReached) {
}
