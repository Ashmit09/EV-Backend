package com.voltx.strategy.notification;

import java.time.LocalDateTime;

public record Notification(
        String recipientName,
        String recipientEmail,
        String recipientPhone,
        String subject,
        String message,
        LocalDateTime createdAt) {
}
