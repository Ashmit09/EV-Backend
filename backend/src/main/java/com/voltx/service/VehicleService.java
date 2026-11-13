package com.voltx.service;

import com.voltx.dto.request.VehicleRequest;
import com.voltx.dto.response.VehicleResponse;

import java.util.List;

public interface VehicleService {

    VehicleResponse create(Long userId, VehicleRequest request);

    List<VehicleResponse> list(Long userId);

    VehicleResponse get(Long userId, Long vehicleId);

    VehicleResponse update(Long userId, Long vehicleId, VehicleRequest request);

    void delete(Long userId, Long vehicleId);
}
