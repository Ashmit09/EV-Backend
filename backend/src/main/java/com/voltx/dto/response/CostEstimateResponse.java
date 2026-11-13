package com.voltx.dto.response;

import com.voltx.enums.PricingPlan;

import java.math.BigDecimal;

public record CostEstimateResponse(BigDecimal effectivePowerKw, double startBatteryPercentage,
                                   double estimatedEndBatteryPercentage, int targetBatteryPercentage,
                                   BigDecimal estimatedEnergyKwh, PricingPlan pricingPlan,
                                   BigDecimal energyCost, String adjustmentLabel, BigDecimal adjustmentAmount,
                                   BigDecimal subtotal, BigDecimal taxAmount, BigDecimal estimatedTotal,
                                   String simulationModel) {
}
