package com.voltx.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record RevenueAnalyticsResponse(List<MonthlyPoint> monthly, Map<String, BigDecimal> byPricingPlan,
                                       Map<String, BigDecimal> byPaymentMethod, Map<String, BigDecimal> byCity) {
}
