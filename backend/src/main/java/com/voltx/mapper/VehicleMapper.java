package com.voltx.mapper;

import com.voltx.dto.response.VehicleResponse;
import com.voltx.entity.Vehicle;

public final class VehicleMapper {

    private VehicleMapper() {
    }

    public static VehicleResponse toResponse(Vehicle vehicle) {
        return new VehicleResponse(vehicle.getId(), vehicle.getVehicleType(), vehicle.getManufacturer(),
                vehicle.getModel(), vehicle.getDisplayName(), vehicle.getRegistrationNumber(),
                vehicle.getBatteryCapacityKwh(), vehicle.getMaxChargingPowerKw(),
                vehicle.getCurrentBatteryPercentage(),
                vehicle.getSupportedConnectors().stream().sorted().toList());
    }
}
