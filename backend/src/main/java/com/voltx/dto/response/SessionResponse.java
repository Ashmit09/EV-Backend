package com.voltx.dto.response;

import com.voltx.enums.ConnectorType;
import com.voltx.enums.PricingPlan;
import com.voltx.enums.SessionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Charging session view. For an IN_PROGRESS session the battery / energy / cost fields are
 * live values computed by the simulator on the server at request time.
 */
public record SessionResponse(Long id, SessionStatus status, Long reservationId, Long vehicleId,
                              String vehicleName, String registrationNumber, BigDecimal batteryCapacityKwh,
                              Long chargerId, String chargerCode, ConnectorType connectorType,
                              String stationName, String city, LocalDateTime startTime, LocalDateTime endTime,
                              LocalDateTime reservationEndTime, double initialBatteryPercentage,
                              double currentBatteryPercentage, double targetBatteryPercentage,
                              BigDecimal energyKwh, long simulatedMinutes, BigDecimal currentPowerKw,
                              BigDecimal effectivePowerKw, BigDecimal pricePerKwh, PricingPlan pricingPlan,
                              BigDecimal cost, boolean targetReached, Long invoiceId, String customerName,
                              String customerEmail) {
}
