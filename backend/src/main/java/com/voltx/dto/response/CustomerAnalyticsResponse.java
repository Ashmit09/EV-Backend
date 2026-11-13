package com.voltx.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record CustomerAnalyticsResponse(long totalSessions, BigDecimal totalEnergyKwh, BigDecimal totalSpent,
                                        BigDecimal outstandingAmount, double averageSessionMinutes,
                                        long upcomingReservations, long vehicles,
                                        List<MonthlyPoint> monthly, List<SessionResponse> recentSessions) {
}
