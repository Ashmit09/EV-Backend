package com.voltx.dto.response;

import java.time.LocalDateTime;

public record NotificationResponse(String subject, String message, LocalDateTime createdAt) {
}
