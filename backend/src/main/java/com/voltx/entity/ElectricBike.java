package com.voltx.entity;

import com.voltx.enums.ConnectorType;
import com.voltx.enums.VehicleType;
import com.voltx.exception.IncompatibleConnectorException;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.math.BigDecimal;
import java.util.Collection;

/**
 * Electric motorcycle. Can use DC chargers, but not ultra-fast units above 60 kW,
 * which are not rated for small two-wheeler battery packs.
 */
@Entity
@DiscriminatorValue("BIKE")
public class ElectricBike extends Vehicle {

    protected ElectricBike() {
        // required by JPA
    }

    public ElectricBike(User owner, String manufacturer, String model, String registrationNumber,
                        BigDecimal batteryCapacityKwh, BigDecimal maxChargingPowerKw,
                        int currentBatteryPercentage, Collection<ConnectorType> connectors) {
        super(owner, manufacturer, model, registrationNumber, batteryCapacityKwh,
                maxChargingPowerKw, currentBatteryPercentage, connectors);
    }

    @Override
    public VehicleType getVehicleType() {
        return VehicleType.BIKE;
    }

    @Override
    protected BigDecimal minBatteryCapacityKwh() {
        return new BigDecimal("2");
    }

    @Override
    protected BigDecimal maxBatteryCapacityKwh() {
        return new BigDecimal("25");
    }

    @Override
    protected BigDecimal maxSupportedChargingPowerKw() {
        return new BigDecimal("30");
    }

    @Override
    protected void validateTypeSpecificConstraints(ChargingUnit unit) {
        if (unit.getMaxPowerKw().compareTo(ULTRA_FAST_THRESHOLD_KW) > 0) {
            throw new IncompatibleConnectorException("Electric bikes cannot use ultra-fast chargers above "
                    + ULTRA_FAST_THRESHOLD_KW.toPlainString() + " kW (" + unit.getChargerCode() + " is "
                    + unit.getMaxPowerKw().stripTrailingZeros().toPlainString() + " kW)");
        }
    }
}
