package com.voltx.service;

import com.voltx.dto.response.NotificationResponse;
import com.voltx.entity.User;
import com.voltx.enums.NotificationChannel;

import java.util.List;

public interface NotificationService {

    /** Sends on the default channels (in-app and e-mail). */
    void notify(User user, String subject, String message);

    /** Overload: sends only on the channels given. */
    void notify(User user, String subject, String message, NotificationChannel... channels);

    List<NotificationResponse> latestFor(String email);
}
