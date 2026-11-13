package com.voltx.service.impl;

import com.voltx.dto.request.VehicleRequest;
import com.voltx.entity.ElectricBike;
import com.voltx.entity.ElectricCar;
import com.voltx.entity.ElectricScooter;
import com.voltx.entity.User;
import com.voltx.entity.Vehicle;

/**
 * Creates the right {@link Vehicle} subclass for a request. Callers receive a {@code Vehicle}
 * reference and never need to know which concrete class was built.
 */
final class VehicleFactory {

    private VehicleFactory() {
    }

    static Vehicle create(User owner, VehicleRequest r) {
        return switch (r.vehicleType()) {
            case CAR -> new ElectricCar(owner, r.manufacturer(), r.model(), r.registrationNumber(),
                    r.batteryCapacityKwh(), r.maxChargingPowerKw(), r.currentBatteryPercentage(),
                    r.supportedConnectors());
            case BIKE -> new ElectricBike(owner, r.manufacturer(), r.model(), r.registrationNumber(),
                    r.batteryCapacityKwh(), r.maxChargingPowerKw(), r.currentBatteryPercentage(),
                    r.supportedConnectors());
            case SCOOTER -> new ElectricScooter(owner, r.manufacturer(), r.model(), r.registrationNumber(),
                    r.batteryCapacityKwh(), r.maxChargingPowerKw(), r.currentBatteryPercentage(),
                    r.supportedConnectors());
        };
    }
}
