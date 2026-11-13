package com.voltx.dto.response;

import java.math.BigDecimal;

public record StationUtilization(Long stationId, String stationName, String city, int totalChargers,
                                 long busyChargers, double occupancyPercent, long sessionsLast30Days,
                                 BigDecimal energyLast30DaysKwh, BigDecimal revenueLast30Days) {
}
