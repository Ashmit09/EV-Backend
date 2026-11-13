package com.voltx.mapper;

import com.voltx.dto.response.ReservationResponse;
import com.voltx.entity.ChargingUnit;
import com.voltx.entity.Reservation;
import com.voltx.entity.User;
import com.voltx.entity.Vehicle;
import com.voltx.enums.ReservationStatus;

import java.time.LocalDateTime;

public final class ReservationMapper {

    private ReservationMapper() {
    }

    public static ReservationResponse toResponse(Reservation reservation, LocalDateTime now, long startWindowMinutes,
                                                 boolean includeCustomer) {
        Vehicle vehicle = reservation.getVehicle();
        ChargingUnit unit = reservation.getChargingUnit();
        boolean confirmed = reservation.getStatus() == ReservationStatus.CONFIRMED;
        User user = includeCustomer ? reservation.getUser() : null;
        return new ReservationResponse(reservation.getId(), reservation.getStatus(), reservation.getStartTime(),
                reservation.getEndTime(), reservation.getDurationMinutes(), vehicle.getId(),
                vehicle.getDisplayName(), vehicle.getRegistrationNumber(), unit.getId(), unit.getChargerCode(),
                unit.getConnectorType(), unit.getMaxPowerKw(), unit.getPricePerKwh(), unit.getStation().getId(),
                unit.getStation().getName(), unit.getStation().getCity(),
                confirmed && reservation.isWithinStartWindow(now, startWindowMinutes),
                confirmed, reservation.getCancellationReason(), reservation.getCreatedAt(),
                user == null ? null : user.getFullName(), user == null ? null : user.getEmail());
    }
}
