package com.voltx.dto.request;

import com.voltx.enums.ChargerAdminAction;
import jakarta.validation.constraints.NotNull;

public record ChargerStatusRequest(@NotNull ChargerAdminAction action) {
}
