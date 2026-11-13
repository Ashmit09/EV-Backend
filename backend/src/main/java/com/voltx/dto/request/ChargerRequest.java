package com.voltx.dto.request;

import com.voltx.enums.ConnectorType;
import com.voltx.enums.PricingPlan;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record ChargerRequest(
        @NotNull Long stationId,
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9-]{3,30}$", message = "must be 3-30 letters, digits or dashes")
        String chargerCode,
        @NotNull ConnectorType connectorType,
        @NotNull @DecimalMin("1.0") @DecimalMax("400.0") BigDecimal maxPowerKw,
        @NotNull @DecimalMin("1.0") @DecimalMax("100.0") BigDecimal pricePerKwh,
        @NotNull PricingPlan pricingPlan) {
}
