package com.voltx.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record AdminOverviewResponse(long totalUsers, long totalCustomers, long totalStations, long activeStations,
                                    long totalChargers, long availableChargers, long activeSessions,
                                    long reservationsToday, BigDecimal totalEnergyKwh, BigDecimal totalRevenue,
                                    BigDecimal collectedRevenue, BigDecimal pendingRevenue,
                                    Map<String, Long> chargerStatusDistribution,
                                    List<StationUtilization> stationUtilization) {
}
