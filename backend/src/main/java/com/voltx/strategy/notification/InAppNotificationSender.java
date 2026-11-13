package com.voltx.strategy.notification;

import com.voltx.enums.NotificationChannel;
import org.springframework.stereotype.Component;

/** In-app channel: stores the notification so the web app's bell icon can show it. */
@Component
public class InAppNotificationSender implements NotificationSender {

    private final InAppNotificationStore store;

    public InAppNotificationSender(InAppNotificationStore store) {
        this.store = store;
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.IN_APP;
    }

    @Override
    public void sendNotification(Notification notification) {
        store.add(notification.recipientEmail(), notification);
    }
}
