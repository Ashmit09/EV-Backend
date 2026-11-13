package com.voltx.dto.response;

import com.voltx.enums.ChargerStatus;
import com.voltx.enums.ConnectorType;
import com.voltx.enums.PricingPlan;

import java.math.BigDecimal;

/**
 * Charger view. {@code compatible} / {@code incompatibilityReason} are filled only when the
 * client asked about a specific vehicle; otherwise they are null.
 */
public record ChargerResponse(Long id, Long stationId, String stationName, String city, String chargerCode,
                              ConnectorType connectorType, String connectorLabel, boolean dcFast,
                              BigDecimal maxPowerKw, BigDecimal pricePerKwh, PricingPlan pricingPlan,
                              String pricingPlanLabel, ChargerStatus status, boolean bookable,
                              Boolean compatible, String incompatibilityReason) {
}
