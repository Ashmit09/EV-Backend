package com.voltx.dto.response;

import com.voltx.enums.ConnectorType;
import com.voltx.enums.StationStatus;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

public record StationResponse(Long id, String name, String address, String city, BigDecimal latitude,
                              BigDecimal longitude, LocalTime openingTime, LocalTime closingTime,
                              boolean open24Hours, StationStatus status, int totalChargers,
                              long availableChargers, List<ConnectorType> connectorTypes,
                              BigDecimal maxPowerKw, BigDecimal minPricePerKwh,
                              List<ChargerResponse> chargers) {
}
