package com.voltx.entity;

import com.voltx.enums.ConnectorType;
import com.voltx.enums.VehicleType;
import com.voltx.exception.IncompatibleConnectorException;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.math.BigDecimal;
import java.util.Collection;

/**
 * Electric scooter. Scooters charge through a small on-board AC charger,
 * so every DC charger is rejected regardless of connector.
 */
@Entity
@DiscriminatorValue("SCOOTER")
public class ElectricScooter extends Vehicle {

    protected ElectricScooter() {
        // required by JPA
    }

    public ElectricScooter(User owner, String manufacturer, String model, String registrationNumber,
                           BigDecimal batteryCapacityKwh, BigDecimal maxChargingPowerKw,
                           int currentBatteryPercentage, Collection<ConnectorType> connectors) {
        super(owner, manufacturer, model, registrationNumber, batteryCapacityKwh,
                maxChargingPowerKw, currentBatteryPercentage, connectors);
    }

    @Override
    public VehicleType getVehicleType() {
        return VehicleType.SCOOTER;
    }

    @Override
    protected BigDecimal minBatteryCapacityKwh() {
        return new BigDecimal("1");
    }

    @Override
    protected BigDecimal maxBatteryCapacityKwh() {
        return new BigDecimal("8");
    }

    @Override
    protected BigDecimal maxSupportedChargingPowerKw() {
        return new BigDecimal("7.4");
    }

    @Override
    protected void validateTypeSpecificConstraints(ChargingUnit unit) {
        if (unit.isDcFastCharger()) {
            throw new IncompatibleConnectorException("Electric scooters support AC charging only; "
                    + unit.getChargerCode() + " is a DC fast charger");
        }
    }
}
