package com.voltx.mapper;

import com.voltx.dto.response.ChargerResponse;
import com.voltx.dto.response.StationResponse;
import com.voltx.entity.ChargingStation;
import com.voltx.entity.ChargingUnit;
import com.voltx.entity.Vehicle;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class StationMapper {

    private StationMapper() {
    }

    public static StationResponse toResponse(ChargingStation station, boolean includeChargers, Vehicle vehicle) {
        List<ChargingUnit> units = station.getChargingUnits();
        List<ChargerResponse> chargers = includeChargers
                ? units.stream().map(unit -> toChargerResponse(unit, vehicle)).toList()
                : List.of();
        return new StationResponse(station.getId(), station.getName(), station.getAddress(), station.getCity(),
                station.getLatitude(), station.getLongitude(), station.getOpeningTime(), station.getClosingTime(),
                station.isOpen24Hours(), station.getStatus(), units.size(), station.countAvailableUnits(),
                units.stream().map(ChargingUnit::getConnectorType).distinct().sorted().toList(),
                units.stream().map(ChargingUnit::getMaxPowerKw).max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO),
                units.stream().map(ChargingUnit::getPricePerKwh).min(Comparator.naturalOrder()).orElse(BigDecimal.ZERO),
                chargers);
    }

    /** @param vehicle optional; when present the response states whether the vehicle can use this charger */
    public static ChargerResponse toChargerResponse(ChargingUnit unit, Vehicle vehicle) {
        Optional<String> issue = vehicle == null ? Optional.empty() : vehicle.findCompatibilityIssue(unit);
        ChargingStation station = unit.getStation();
        return new ChargerResponse(unit.getId(), station.getId(), station.getName(), station.getCity(),
                unit.getChargerCode(), unit.getConnectorType(), unit.getConnectorType().getLabel(),
                unit.isDcFastCharger(), unit.getMaxPowerKw(), unit.getPricePerKwh(), unit.getPricingPlan(),
                unit.getPricingPlan().getLabel(), unit.getStatus(), unit.acceptsBookings(),
                vehicle == null ? null : issue.isEmpty(), issue.orElse(null));
    }
}
