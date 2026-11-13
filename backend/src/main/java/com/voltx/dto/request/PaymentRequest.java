package com.voltx.dto.request;

import com.voltx.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;

public record PaymentRequest(@NotNull PaymentMethod method) {
}
