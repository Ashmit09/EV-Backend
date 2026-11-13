package com.voltx.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record EnergyAnalyticsResponse(List<MonthlyPoint> monthly, Map<String, BigDecimal> byConnector,
                                      Map<String, BigDecimal> byVehicleType, Map<String, BigDecimal> byCity) {
}
