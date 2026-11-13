package com.voltx.dto.response;

import com.voltx.enums.ConnectorType;
import com.voltx.enums.ReservationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReservationResponse(Long id, ReservationStatus status, LocalDateTime startTime,
                                  LocalDateTime endTime, long durationMinutes, Long vehicleId,
                                  String vehicleName, String registrationNumber, Long chargerId,
                                  String chargerCode, ConnectorType connectorType, BigDecimal maxPowerKw,
                                  BigDecimal pricePerKwh, Long stationId, String stationName, String city,
                                  boolean canStart, boolean canCancel, String cancellationReason,
                                  LocalDateTime createdAt, String customerName, String customerEmail) {
}
