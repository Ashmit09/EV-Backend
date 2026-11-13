package com.voltx.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

public record ReservationRequest(
        @NotNull Long vehicleId,
        @NotNull Long chargingUnitId,
        @NotNull LocalDateTime startTime,
        @NotNull @Positive Integer durationMinutes) {
}
