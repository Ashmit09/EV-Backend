package com.voltx.dto.response;

import com.voltx.enums.ConnectorType;
import com.voltx.enums.VehicleType;

import java.math.BigDecimal;
import java.util.List;

public record VehicleResponse(Long id, VehicleType vehicleType, String manufacturer, String model,
                              String displayName, String registrationNumber, BigDecimal batteryCapacityKwh,
                              BigDecimal maxChargingPowerKw, int currentBatteryPercentage,
                              List<ConnectorType> supportedConnectors) {
}
