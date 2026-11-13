package com.voltx.dto.request;

import com.voltx.enums.ConnectorType;
import com.voltx.enums.VehicleType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Set;

public record VehicleRequest(
        @NotNull VehicleType vehicleType,
        @NotBlank @Size(max = 60) String manufacturer,
        @NotBlank @Size(max = 60) String model,
        @NotBlank
        @Pattern(regexp = "^[A-Za-z]{2}[ -]?[0-9]{1,2}[ -]?[A-Za-z]{0,3}[ -]?[0-9]{1,4}$",
                message = "must look like an Indian registration number, e.g. KA01AB1234")
        String registrationNumber,
        @NotNull @DecimalMin(value = "1.0") @DecimalMax(value = "200.0") BigDecimal batteryCapacityKwh,
        @NotNull @DecimalMin(value = "0.5") @DecimalMax(value = "350.0") BigDecimal maxChargingPowerKw,
        @NotNull @Min(0) @Max(100) Integer currentBatteryPercentage,
        @NotEmpty Set<ConnectorType> supportedConnectors) {
}
