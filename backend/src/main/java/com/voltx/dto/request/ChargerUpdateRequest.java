package com.voltx.dto.request;

import com.voltx.enums.ConnectorType;
import com.voltx.enums.PricingPlan;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ChargerUpdateRequest(
        @NotNull ConnectorType connectorType,
        @NotNull @DecimalMin("1.0") @DecimalMax("400.0") BigDecimal maxPowerKw,
        @NotNull @DecimalMin("1.0") @DecimalMax("100.0") BigDecimal pricePerKwh,
        @NotNull PricingPlan pricingPlan) {
}
