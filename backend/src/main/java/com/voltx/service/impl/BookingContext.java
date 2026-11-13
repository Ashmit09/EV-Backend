package com.voltx.service.impl;

import com.voltx.entity.ChargingUnit;
import com.voltx.entity.Vehicle;

import java.time.LocalDateTime;

/** The facts a booking rule needs, bundled into one immutable record. */
public record BookingContext(Vehicle vehicle, ChargingUnit unit, LocalDateTime start, LocalDateTime end,
                             int durationMinutes, LocalDateTime now) {
}
