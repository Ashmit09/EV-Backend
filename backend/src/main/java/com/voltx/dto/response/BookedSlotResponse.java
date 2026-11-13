package com.voltx.dto.response;

import java.time.LocalDateTime;

/** An occupied window on a charger. Carries no customer information. */
public record BookedSlotResponse(LocalDateTime startTime, LocalDateTime endTime) {
}
