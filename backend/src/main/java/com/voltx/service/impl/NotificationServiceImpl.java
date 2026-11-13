package com.voltx.service.impl;

import com.voltx.dto.response.NotificationResponse;
import com.voltx.entity.User;
import com.voltx.enums.NotificationChannel;
import com.voltx.service.NotificationService;
import com.voltx.strategy.notification.InAppNotificationStore;
import com.voltx.strategy.notification.Notification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final NotificationChannel[] DEFAULT_CHANNELS = {NotificationChannel.IN_APP, NotificationChannel.EMAIL};

    private final AsyncNotificationDispatcher dispatcher;
    private final InAppNotificationStore inAppStore;
    private final Clock clock;

    public NotificationServiceImpl(AsyncNotificationDispatcher dispatcher, InAppNotificationStore inAppStore,
                                   Clock clock) {
        this.dispatcher = dispatcher;
        this.inAppStore = inAppStore;
        this.clock = clock;
    }

    @Override
    public void notify(User user, String subject, String message) {
        notify(user, subject, message, DEFAULT_CHANNELS);
    }

    @Override
    public void notify(User user, String subject, String message, NotificationChannel... channels) {
        // Read the entity now, on the request thread, while the persistence context is open.
        Notification notification = new Notification(user.getFullName(), user.getEmail(), user.getPhone(),
                subject, message, LocalDateTime.now(clock));
        Set<NotificationChannel> targets = channels.length == 0
                ? EnumSet.copyOf(List.of(DEFAULT_CHANNELS))
                : EnumSet.copyOf(List.of(channels));

        // Only notify once the business transaction has actually committed.
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dispatcher.dispatch(notification, targets);
                }
            });
        } else {
            dispatcher.dispatch(notification, targets);
        }
    }

    @Override
    public List<NotificationResponse> latestFor(String email) {
        return inAppStore.latest(email).stream()
                .map(n -> new NotificationResponse(n.subject(), n.message(), n.createdAt()))
                .toList();
    }
}
