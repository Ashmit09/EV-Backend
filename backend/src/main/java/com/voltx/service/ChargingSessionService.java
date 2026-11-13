package com.voltx.service;

import com.voltx.dto.request.StartSessionRequest;
import com.voltx.dto.response.SessionResponse;

import java.util.List;
import java.util.Optional;

public interface ChargingSessionService {

    SessionResponse start(Long userId, StartSessionRequest request);

    Optional<SessionResponse> active(Long userId);

    SessionResponse get(Long userId, Long sessionId);

    SessionResponse stop(Long userId, Long sessionId);

    List<SessionResponse> history(Long userId);

    List<SessionResponse> listActive();

    SessionResponse stopAsAdmin(String adminEmail, Long sessionId);

    /** Scheduler: finish sessions that reached their target or whose booked slot ended. */
    int autoCompleteSessions();
}
