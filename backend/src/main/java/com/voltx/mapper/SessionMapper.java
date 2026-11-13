package com.voltx.mapper;

import com.voltx.dto.response.SessionResponse;
import com.voltx.entity.ChargingSession;
import com.voltx.entity.ChargingUnit;
import com.voltx.entity.User;
import com.voltx.entity.Vehicle;
import com.voltx.simulator.ChargingProgress;

import java.math.BigDecimal;

public final class SessionMapper {

    private SessionMapper() {
    }

    /**
     * @param live     live simulator snapshot for an in-progress session, or null for a completed one
     * @param liveCost pre-tax cost of the live snapshot, or null for a completed one
     */
    public static SessionResponse toResponse(ChargingSession session, ChargingProgress live, BigDecimal liveCost,
                                             Long invoiceId, boolean includeCustomer) {
        Vehicle vehicle = session.getVehicle();
        ChargingUnit unit = session.getChargingUnit();
        User user = includeCustomer ? session.getUser() : null;
        boolean active = live != null;
        double current = active ? live.batteryPercentage()
                : session.getFinalBatteryPercentage() != null ? session.getFinalBatteryPercentage()
                : session.getInitialBatteryPercentage();
        return new SessionResponse(session.getId(), session.getStatus(),
                session.getReservation() != null ? session.getReservation().getId() : null,
                vehicle.getId(), vehicle.getDisplayName(), vehicle.getRegistrationNumber(),
                vehicle.getBatteryCapacityKwh(), unit.getId(), unit.getChargerCode(), unit.getConnectorType(),
                unit.getStation().getName(), unit.getStation().getCity(), session.getStartTime(),
                session.getEndTime(),
                session.getReservation() != null ? session.getReservation().getEndTime() : null,
                session.getInitialBatteryPercentage(), current, session.getTargetBatteryPercentage(),
                active ? live.energyDeliveredKwh() : session.getEnergyConsumedKwh(),
                active ? live.simulatedMinutes()
                        : session.getSimulatedDurationMinutes() == null ? 0 : session.getSimulatedDurationMinutes(),
                active ? live.currentPowerKw() : BigDecimal.ZERO,
                session.getEffectivePowerKw(), session.getPricePerKwh(), session.getPricingPlan(),
                active ? liveCost : session.getCost(),
                active ? live.targetReached() : true,
                invoiceId,
                user == null ? null : user.getFullName(), user == null ? null : user.getEmail());
    }
}
