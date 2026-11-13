package com.voltx.strategy.notification;

import com.voltx.enums.NotificationChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Simulated SMS channel. Skips users without a phone number; SMS bodies are truncated to 160 chars. */
@Component
public class SmsNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(SmsNotificationSender.class);
    private static final int SMS_LENGTH = 160;

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.SMS;
    }

    @Override
    public void sendNotification(Notification notification) {
        if (notification.recipientPhone() == null || notification.recipientPhone().isBlank()) {
            log.debug("[SMS] skipped for {} (no phone number)", notification.recipientEmail());
            return;
        }
        String body = "VoltX: " + notification.message();
        if (body.length() > SMS_LENGTH) {
            body = body.substring(0, SMS_LENGTH - 1) + "…";
        }
        log.info("[SMS] to={} body=\"{}\"", notification.recipientPhone(), body);
    }
}
