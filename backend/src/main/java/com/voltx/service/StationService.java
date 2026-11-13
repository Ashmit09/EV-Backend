package com.voltx.service;

import com.voltx.dto.request.StationRequest;
import com.voltx.dto.response.ChargerResponse;
import com.voltx.dto.response.StationResponse;
import com.voltx.enums.ConnectorType;

import java.util.List;

public interface StationService {

    List<StationResponse> search(String city, String keyword, ConnectorType connector, boolean availableOnly);

    List<String> cities();

    /** @param vehicleId optional; when given (and owned by userId) chargers are annotated with compatibility */
    StationResponse get(Long stationId, Long vehicleId, Long userId);

    List<ChargerResponse> chargers(Long stationId, Long vehicleId, Long userId);

    List<StationResponse> listAll();

    StationResponse create(String adminEmail, StationRequest request);

    StationResponse update(String adminEmail, Long stationId, StationRequest request);

    StationResponse setEnabled(String adminEmail, Long stationId, boolean enabled);
}
