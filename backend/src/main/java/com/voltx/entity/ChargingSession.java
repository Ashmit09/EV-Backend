package com.voltx.entity;

import com.voltx.enums.PricingPlan;
import com.voltx.enums.SessionStatus;
import com.voltx.exception.InvalidSessionException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * One charging run of a vehicle on a charging unit.
 * <p>
 * The price per kWh and pricing plan are <b>snapshotted</b> when the session starts, so an
 * administrator changing the tariff mid-session cannot change what the customer is billed.
 * The result fields (final battery, energy, cost) can be written exactly once, through
 * {@link #complete}.
 */
@Entity
@Table(name = "charging_sessions")
public class ChargingSession extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", unique = true)
    private Reservation reservation;

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

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "initial_battery_percentage", nullable = false)
    private double initialBatteryPercentage;

    @Column(name = "target_battery_percentage", nullable = false)
    private double targetBatteryPercentage;

    @Column(name = "final_battery_percentage")
    private Double finalBatteryPercentage;

    @Column(name = "energy_consumed_kwh", precision = 8, scale = 3)
    private BigDecimal energyConsumedKwh;

    @Column(name = "simulated_duration_minutes")
    private Integer simulatedDurationMinutes;

    @Column(name = "effective_power_kw", nullable = false, precision = 6, scale = 2)
    private BigDecimal effectivePowerKw;

    @Column(name = "price_per_kwh", nullable = false, precision = 8, scale = 2)
    private BigDecimal pricePerKwh;

    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_plan", nullable = false, length = 20)
    private PricingPlan pricingPlan;

    @Column(precision = 10, scale = 2)
    private BigDecimal cost;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SessionStatus status = SessionStatus.IN_PROGRESS;

    protected ChargingSession() {
        // required by JPA
    }

    public ChargingSession(Reservation reservation, LocalDateTime startTime, double initialBatteryPercentage,
                           double targetBatteryPercentage, BigDecimal effectivePowerKw) {
        this.reservation = Objects.requireNonNull(reservation);
        this.user = reservation.getUser();
        this.vehicle = reservation.getVehicle();
        this.chargingUnit = reservation.getChargingUnit();
        this.pricePerKwh = chargingUnit.getPricePerKwh();
        this.pricingPlan = chargingUnit.getPricingPlan();
        this.startTime = Objects.requireNonNull(startTime);
        if (initialBatteryPercentage < 0 || initialBatteryPercentage >= 100) {
            throw new InvalidSessionException("Battery is already full; nothing to charge");
        }
        if (targetBatteryPercentage <= initialBatteryPercentage || targetBatteryPercentage > 100) {
            throw new InvalidSessionException(String.format(
                    "Target battery %.0f%% must be above the current level %.0f%% and at most 100%%",
                    targetBatteryPercentage, initialBatteryPercentage));
        }
        if (effectivePowerKw == null || effectivePowerKw.signum() <= 0) {
            throw new InvalidSessionException("Effective charging power must be positive");
        }
        this.initialBatteryPercentage = initialBatteryPercentage;
        this.targetBatteryPercentage = targetBatteryPercentage;
        this.effectivePowerKw = effectivePowerKw;
    }

    /** Writes the final figures. Can only happen once, while the session is in progress. */
    public void complete(LocalDateTime endTime, double finalBatteryPercentage, BigDecimal energyConsumedKwh,
                         int simulatedDurationMinutes, BigDecimal cost) {
        if (status != SessionStatus.IN_PROGRESS) {
            throw new InvalidSessionException("Charging session #" + getId() + " has already been completed");
        }
        if (finalBatteryPercentage < initialBatteryPercentage || finalBatteryPercentage > 100) {
            throw new InvalidSessionException("Final battery percentage is out of range");
        }
        if (endTime.isBefore(startTime)) {
            throw new InvalidSessionException("A session cannot end before it starts");
        }
        this.endTime = endTime;
        this.finalBatteryPercentage = finalBatteryPercentage;
        this.energyConsumedKwh = energyConsumedKwh;
        this.simulatedDurationMinutes = simulatedDurationMinutes;
        this.cost = cost;
        this.status = SessionStatus.COMPLETED;
    }

    public boolean isActive() {
        return status == SessionStatus.IN_PROGRESS;
    }

    public boolean isOwnedBy(Long userId) {
        return user.getId() != null && user.getId().equals(userId);
    }

    public Reservation getReservation() {
        return reservation;
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

    public double getInitialBatteryPercentage() {
        return initialBatteryPercentage;
    }

    public double getTargetBatteryPercentage() {
        return targetBatteryPercentage;
    }

    public Double getFinalBatteryPercentage() {
        return finalBatteryPercentage;
    }

    public BigDecimal getEnergyConsumedKwh() {
        return energyConsumedKwh;
    }

    public Integer getSimulatedDurationMinutes() {
        return simulatedDurationMinutes;
    }

    public BigDecimal getEffectivePowerKw() {
        return effectivePowerKw;
    }

    public BigDecimal getPricePerKwh() {
        return pricePerKwh;
    }

    public PricingPlan getPricingPlan() {
        return pricingPlan;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public SessionStatus getStatus() {
        return status;
    }

    /** Allows the demo-data seeder to create historical records with realistic timestamps. */
    public void backdate(LocalDateTime createdAt) {
        backdateCreation(createdAt);
    }
}
