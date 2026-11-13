package com.voltx.dto.request;

import jakarta.validation.constraints.Size;

public record CancelReservationRequest(@Size(max = 255) String reason) {
}
