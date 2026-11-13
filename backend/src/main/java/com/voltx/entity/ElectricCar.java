package com.voltx.entity;

import com.voltx.enums.ConnectorType;
import com.voltx.enums.VehicleType;
import com.voltx.exception.IncompatibleConnectorException;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.math.BigDecimal;
import java.util.Collection;

/**
 * Four-wheeler EV (e.g. Tata Nexon EV, MG ZS EV).
 * Cars with a small on-board charger (below 25 kW) cannot use DC fast chargers, and a car
 * stops DC fast-charging at 80% by default (charging above 80% is slow and wears the battery).
 */
@Entity
@DiscriminatorValue("CAR")
public class ElectricCar extends Vehicle {

    private static final BigDecimal MIN_DC_CAPABLE_POWER_KW = new BigDecimal("25");

    protected ElectricCar() {
        // required by JPA
    }

    public ElectricCar(User owner, String manufacturer, String model, String registrationNumber,
                       BigDecimal batteryCapacityKwh, BigDecimal maxChargingPowerKw,
                       int currentBatteryPercentage, Collection<ConnectorType> connectors) {
        super(owner, manufacturer, model, registrationNumber, batteryCapacityKwh,
                maxChargingPowerKw, currentBatteryPercentage, connectors);
    }

    @Override
    public VehicleType getVehicleType() {
        return VehicleType.CAR;
    }

    @Override
    protected BigDecimal minBatteryCapacityKwh() {
        return new BigDecimal("15");
    }

    @Override
    protected BigDecimal maxBatteryCapacityKwh() {
        return new BigDecimal("200");
    }

    @Override
    protected BigDecimal maxSupportedChargingPowerKw() {
        return new BigDecimal("350");
    }

    @Override
    protected void validateTypeSpecificConstraints(ChargingUnit unit) {
        if (unit.isDcFastCharger() && getMaxChargingPowerKw().compareTo(MIN_DC_CAPABLE_POWER_KW) < 0) {
            throw new IncompatibleConnectorException(getDisplayName()
                    + " only supports AC charging and cannot use DC fast charger " + unit.getChargerCode());
        }
    }

    @Override
    public int defaultTargetPercentage(ChargingUnit unit) {
        return unit.isDcFastCharger() ? 80 : 100;
    }
}
