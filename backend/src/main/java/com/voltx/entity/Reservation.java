package com.voltx.entity;

import com.voltx.enums.ReservationStatus;
import com.voltx.exception.InvalidBookingException;
import com.voltx.exception.InvalidSessionException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * A booked time slot on a charging unit for one vehicle.
 * Lifecycle: CONFIRMED → ACTIVE → COMPLETED, or CONFIRMED → CANCELLED / EXPIRED.
 */
@Entity
@Table(name = "reservations")
public class Reservation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "charging_unit_id", nullable = false)
    private ChargingUnit chargingUnit;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status = ReservationStatus.CONFIRMED;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    protected Reservation() {
        // required by JPA
    }

    public Reservation(User user, Vehicle vehicle, ChargingUnit chargingUnit,
                       LocalDateTime startTime, LocalDateTime endTime) {
        this.user = Objects.requireNonNull(user);
        this.vehicle = Objects.requireNonNull(vehicle);
        this.chargingUnit = Objects.requireNonNull(chargingUnit);
        if (startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            throw new InvalidBookingException("Reservation end time must be after its start time");
        }
        this.startTime = startTime;
        this.endTime = endTime;
    }

    // ------------------------------------------------------------------ lifecycle

    public void cancel(String reason, LocalDateTime at) {
        if (status != ReservationStatus.CONFIRMED) {
            throw new InvalidBookingException("Only confirmed reservations can be cancelled (current: " + status + ")");
        }
        this.status = ReservationStatus.CANCELLED;
        this.cancellationReason = reason;
        this.cancelledAt = at;
    }

    /** Called when the customer starts charging against this booking. */
    public void activate() {
        if (status != ReservationStatus.CONFIRMED) {
            throw new InvalidSessionException("Reservation #" + getId() + " is " + status + " and cannot be started");
        }
        this.status = ReservationStatus.ACTIVE;
    }

    public void complete() {
        if (status != ReservationStatus.ACTIVE) {
            throw new InvalidSessionException("Reservation #" + getId() + " is not active");
        }
        this.status = ReservationStatus.COMPLETED;
    }

    /** No-show: the slot ended without a session being started. */
    public void expire() {
        if (status != ReservationStatus.CONFIRMED) {
            throw new InvalidBookingException("Only confirmed reservations can expire");
        }
        this.status = ReservationStatus.EXPIRED;
    }

    // ------------------------------------------------------------------ queries

    /** Charging may begin a few minutes early and any time before the slot ends. */
    public boolean isWithinStartWindow(LocalDateTime now, long earlyStartMinutes) {
        return !now.isBefore(startTime.minusMinutes(earlyStartMinutes)) && now.isBefore(endTime);
    }

    public boolean overlaps(LocalDateTime otherStart, LocalDateTime otherEnd) {
        return startTime.isBefore(otherEnd) && endTime.isAfter(otherStart);
    }

    public boolean isOwnedBy(Long userId) {
        return user.getId() != null && user.getId().equals(userId);
    }

    public long getDurationMinutes() {
        return Duration.between(startTime, endTime).toMinutes();
    }

    public User getUser() {
        return user;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public ChargingUnit getChargingUnit() {
        return chargingUnit;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    /** Allows the demo-data seeder to create historical records with realistic timestamps. */
    public void backdate(LocalDateTime createdAt) {
        backdateCreation(createdAt);
    }
}
