package com.voltx.strategy.notification;

import com.voltx.enums.NotificationChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Simulated e-mail channel: writes the message to the application log instead of an SMTP server. */
@Component
public class EmailNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationSender.class);

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public void sendNotification(Notification notification) {
        log.info("[EMAIL] to={} subject=\"{}\" body=\"{}\"",
                notification.recipientEmail(), notification.subject(), notification.message());
    }
}
