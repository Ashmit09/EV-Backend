package com.voltx.entity;

import com.voltx.enums.ChargerStatus;
import com.voltx.enums.ConnectorType;
import com.voltx.enums.PricingPlan;
import com.voltx.exception.BusinessRuleViolationException;
import com.voltx.exception.InvalidChargingStateException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Objects;

/**
 * A single charging point at a station.
 *
 * <h3>Encapsulation</h3>
 * The {@code status} field has <b>no setter</b>. The only way to change it is through
 * intention-revealing methods ({@link #reserve()}, {@link #startCharging()},
 * {@link #completeCharging()}, {@link #markOutOfService()} ...). Every one of them goes through
 * {@link #transitionTo(ChargerStatus)}, which consults the transition table in
 * {@link ChargerStatus} and throws {@link InvalidChargingStateException} for illegal moves.
 * A controller therefore cannot put a charger into an inconsistent state.
 */
@Entity
@Table(name = "charging_units")
public class ChargingUnit extends BaseEntity {

    private static final BigDecimal MAX_POWER_KW = new BigDecimal("400");
    private static final BigDecimal MAX_PRICE_PER_KWH = new BigDecimal("100");

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "station_id", nullable = false)
    private ChargingStation station;

    @Column(name = "charger_code", nullable = false, unique = true, length = 30)
    private String chargerCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "connector_type", nullable = false, length = 20)
    private ConnectorType connectorType;

    @Column(name = "max_power_kw", nullable = false, precision = 6, scale = 2)
    private BigDecimal maxPowerKw;

    @Column(name = "price_per_kwh", nullable = false, precision = 8, scale = 2)
    private BigDecimal pricePerKwh;

    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_plan", nullable = false, length = 20)
    private PricingPlan pricingPlan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChargerStatus status = ChargerStatus.AVAILABLE;

    /** Optimistic-locking version; a stale concurrent update fails instead of overwriting. */
    @Version
    private long version;

    protected ChargingUnit() {
        // required by JPA
    }

    public ChargingUnit(ChargingStation station, String chargerCode, ConnectorType connectorType,
                        BigDecimal maxPowerKw, BigDecimal pricePerKwh, PricingPlan pricingPlan) {
        this.station = Objects.requireNonNull(station, "Station is required");
        if (chargerCode == null || chargerCode.isBlank()) {
            throw new BusinessRuleViolationException("Charger code must not be blank");
        }
        this.chargerCode = chargerCode.trim().toUpperCase(Locale.ROOT);
        updateSpecifications(connectorType, maxPowerKw);
        updatePricing(pricePerKwh, pricingPlan);
        station.addChargingUnit(this);
    }

    // ------------------------------------------------------------------ state machine

    public void reserve() {
        ensureStationActive();
        transitionTo(ChargerStatus.RESERVED);
    }

    public void startCharging() {
        ensureStationActive();
        transitionTo(ChargerStatus.CHARGING);
    }

    public void completeCharging() {
        transitionTo(ChargerStatus.AVAILABLE);
    }

    /** Called when the reservation holding this charger is cancelled or expires. */
    public void releaseReservation() {
        if (status != ChargerStatus.RESERVED) {
            throw new InvalidChargingStateException(
                    "Charger " + chargerCode + " is " + status + " and holds no reservation to release");
        }
        transitionTo(ChargerStatus.AVAILABLE);
    }

    public void markOutOfService() {
        transitionTo(ChargerStatus.OUT_OF_SERVICE);
    }

    public void markUnderMaintenance() {
        transitionTo(ChargerStatus.MAINTENANCE);
    }

    /** Brings an out-of-service or maintenance charger back into operation. */
    public void restoreService() {
        if (status != ChargerStatus.OUT_OF_SERVICE && status != ChargerStatus.MAINTENANCE) {
            throw new InvalidChargingStateException(
                    "Charger " + chargerCode + " is already in service (" + status + ")");
        }
        transitionTo(ChargerStatus.AVAILABLE);
    }

    private void transitionTo(ChargerStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new InvalidChargingStateException(String.format(
                    "Charger %s cannot move from %s to %s", chargerCode, status, target));
        }
        this.status = target;
    }

    private void ensureStationActive() {
        if (!station.isActive()) {
            throw new InvalidChargingStateException("Station " + station.getName() + " is disabled");
        }
    }

    // ------------------------------------------------------------------ configuration

    public void updateSpecifications(ConnectorType connectorType, BigDecimal maxPowerKw) {
        this.connectorType = Objects.requireNonNull(connectorType, "Connector type is required");
        if (maxPowerKw == null || maxPowerKw.signum() <= 0 || maxPowerKw.compareTo(MAX_POWER_KW) > 0) {
            throw new BusinessRuleViolationException("Charger power must be between 0 and 400 kW");
        }
        if (!connectorType.isDirectCurrent() && maxPowerKw.compareTo(new BigDecimal("43")) > 0) {
            throw new BusinessRuleViolationException("AC (Type 2) chargers cannot exceed 43 kW");
        }
        this.maxPowerKw = maxPowerKw;
    }

    public void updatePricing(BigDecimal pricePerKwh, PricingPlan pricingPlan) {
        if (pricePerKwh == null || pricePerKwh.signum() <= 0 || pricePerKwh.compareTo(MAX_PRICE_PER_KWH) > 0) {
            throw new BusinessRuleViolationException("Price per kWh must be between 0 and 100");
        }
        this.pricePerKwh = pricePerKwh;
        this.pricingPlan = Objects.requireNonNull(pricingPlan, "Pricing plan is required");
    }

    // ------------------------------------------------------------------ queries

    /** DC connectors deliver power straight to the battery and count as fast chargers. */
    public boolean isDcFastCharger() {
        return connectorType.isDirectCurrent();
    }

    /** Whether new bookings may be placed on this unit (time-slot overlap is checked separately). */
    public boolean acceptsBookings() {
        return station.isActive() && status.acceptsBookings();
    }

    public ChargingStation getStation() {
        return station;
    }

    public String getChargerCode() {
        return chargerCode;
    }

    public ConnectorType getConnectorType() {
        return connectorType;
    }

    public BigDecimal getMaxPowerKw() {
        return maxPowerKw;
    }

    public BigDecimal getPricePerKwh() {
        return pricePerKwh;
    }

    public PricingPlan getPricingPlan() {
        return pricingPlan;
    }

    public ChargerStatus getStatus() {
        return status;
    }

    public long getVersion() {
        return version;
    }
}
