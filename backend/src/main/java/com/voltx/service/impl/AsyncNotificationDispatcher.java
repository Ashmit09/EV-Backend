package com.voltx.service.impl;

import com.voltx.enums.NotificationChannel;
import com.voltx.strategy.notification.Notification;
import com.voltx.strategy.notification.NotificationSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * Delivers notifications on a background thread pool ({@code notificationExecutor}),
 * so a slow e-mail/SMS provider can never slow down a booking or a session stop.
 * <p>
 * Runtime polymorphism: the loop calls {@code sender.sendNotification(...)} on the
 * {@link NotificationSender} interface; e-mail, SMS and in-app each run their own override.
 */
@Component
public class AsyncNotificationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(AsyncNotificationDispatcher.class);

    private final List<NotificationSender> senders;

    public AsyncNotificationDispatcher(List<NotificationSender> senders) {
        this.senders = List.copyOf(senders);
    }

    @Async("notificationExecutor")
    public void dispatch(Notification notification, Set<NotificationChannel> channels) {
        senders.stream()
                .filter(sender -> channels.contains(sender.channel()))
                .forEach(sender -> {
                    try {
                        sender.sendNotification(notification);
                    } catch (RuntimeException ex) {
                        log.warn("{} notification failed: {}", sender.channel(), ex.getMessage());
                    }
                });
    }
}
