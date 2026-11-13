package com.voltx.strategy.notification;

import com.voltx.enums.NotificationChannel;

/** One delivery channel. Implementations are selected at runtime by channel. */
public interface NotificationSender {

    NotificationChannel channel();

    void sendNotification(Notification notification);
}
