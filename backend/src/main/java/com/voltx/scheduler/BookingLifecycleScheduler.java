package com.voltx.scheduler;

import com.voltx.service.ChargingSessionService;
import com.voltx.service.ReservationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Background job (runs on Spring's scheduler thread) that keeps charger states in step with time:
 * holds a charger when its booked slot is about to begin, expires no-shows, and finishes
 * sessions that reached their target or whose slot has ended.
 */
@Component
public class BookingLifecycleScheduler {

    private static final Logger log = LoggerFactory.getLogger(BookingLifecycleScheduler.class);

    private final ReservationService reservationService;
    private final ChargingSessionService sessionService;

    public BookingLifecycleScheduler(ReservationService reservationService, ChargingSessionService sessionService) {
        this.reservationService = reservationService;
        this.sessionService = sessionService;
    }

    @Scheduled(fixedDelayString = "${voltx.scheduler.interval-ms:15000}", initialDelay = 5000)
    public void run() {
        runSafely("auto-complete sessions", sessionService::autoCompleteSessions);
        runSafely("expire reservations", reservationService::expireMissedReservations);
        runSafely("activate reservations", reservationService::activateDueReservations);
    }

    /** Each step runs in its own transaction; one failing step must not stop the others. */
    private void runSafely(String name, java.util.function.IntSupplier step) {
        try {
            step.getAsInt();
        } catch (RuntimeException ex) {
            log.warn("Scheduler step '{}' failed: {}", name, ex.getMessage());
        }
    }
}
