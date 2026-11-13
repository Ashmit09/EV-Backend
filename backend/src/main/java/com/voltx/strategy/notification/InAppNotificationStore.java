package com.voltx.strategy.notification;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Thread-safe, in-memory inbox of the latest notifications per user.
 * Notifications are produced on background threads (see {@code @Async} in NotificationServiceImpl)
 * and read by request threads, so concurrent collections are required here.
 */
@Component
public class InAppNotificationStore {

    private static final int MAX_PER_USER = 20;

    private final Map<String, Deque<Notification>> inbox = new ConcurrentHashMap<>();

    public void add(String email, Notification notification) {
        Deque<Notification> queue = inbox.computeIfAbsent(email, key -> new ConcurrentLinkedDeque<>());
        queue.addFirst(notification);
        while (queue.size() > MAX_PER_USER) {
            queue.pollLast();
        }
    }

    public List<Notification> latest(String email) {
        return new ArrayList<>(inbox.getOrDefault(email, new ConcurrentLinkedDeque<>()));
    }
}
