package com.voltx.service;

import com.voltx.dto.request.ReservationRequest;
import com.voltx.dto.response.CostEstimateResponse;
import com.voltx.dto.response.ReservationResponse;
import com.voltx.enums.ReservationStatus;

import java.util.List;

public interface ReservationService {

    ReservationResponse create(Long userId, ReservationRequest request);

    CostEstimateResponse estimate(Long userId, ReservationRequest request);

    List<ReservationResponse> myReservations(Long userId);

    ReservationResponse get(Long userId, Long reservationId);

    ReservationResponse cancel(Long userId, Long reservationId, String reason);

    List<ReservationResponse> listAll(ReservationStatus status);

    ReservationResponse cancelAsAdmin(String adminEmail, Long reservationId, String reason);

    int cancelUpcomingForCharger(Long chargerId, String reason, String actorEmail);

    int cancelUpcomingForStation(Long stationId, String reason, String actorEmail);

    /** Scheduler: hold chargers whose booked slot is about to begin. */
    int activateDueReservations();

    /** Scheduler: expire bookings whose slot ended without a session (no-shows). */
    int expireMissedReservations();
}
