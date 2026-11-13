package com.voltx.entity;

import com.voltx.enums.ChargerStatus;
import com.voltx.enums.StationStatus;
import com.voltx.exception.BusinessRuleViolationException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A physical charging location that groups several {@link ChargingUnit}s.
 * The list of units is exposed read-only; new units are attached through
 * {@link #addChargingUnit(ChargingUnit)} so both sides of the relationship stay consistent.
 */
@Entity
@Table(name = "charging_stations")
public class ChargingStation extends BaseEntity {

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false, length = 80)
    private String city;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(name = "opening_time", nullable = false)
    private LocalTime openingTime;

    @Column(name = "closing_time", nullable = false)
    private LocalTime closingTime;

    @Column(name = "open_24_hours", nullable = false)
    private boolean open24Hours;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StationStatus status = StationStatus.ACTIVE;

    @OneToMany(mappedBy = "station", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("chargerCode ASC")
    private List<ChargingUnit> chargingUnits = new ArrayList<>();

    protected ChargingStation() {
        // required by JPA
    }

    public ChargingStation(String name, String address, String city, BigDecimal latitude, BigDecimal longitude,
                           LocalTime openingTime, LocalTime closingTime, boolean open24Hours) {
        updateDetails(name, address, city, latitude, longitude, openingTime, closingTime, open24Hours);
    }

    public void updateDetails(String name, String address, String city, BigDecimal latitude,
                                    BigDecimal longitude, LocalTime openingTime, LocalTime closingTime,
                                    boolean open24Hours) {
        if (!open24Hours && (openingTime == null || closingTime == null || !openingTime.isBefore(closingTime))) {
            throw new BusinessRuleViolationException("Opening time must be before closing time");
        }
        this.name = name.trim();
        this.address = address.trim();
        this.city = city.trim();
        this.latitude = latitude;
        this.longitude = longitude;
        this.open24Hours = open24Hours;
        this.openingTime = open24Hours ? LocalTime.MIDNIGHT : openingTime;
        this.closingTime = open24Hours ? LocalTime.of(23, 59) : closingTime;
    }

    /** True when the whole booking window falls inside today's operating hours. */
    public boolean isOpenDuring(LocalDateTime start, LocalDateTime end) {
        if (open24Hours) {
            return true;
        }
        boolean sameDay = start.toLocalDate().equals(end.toLocalDate());
        return sameDay
                && !start.toLocalTime().isBefore(openingTime)
                && !end.toLocalTime().isAfter(closingTime);
    }

    public void addChargingUnit(ChargingUnit unit) {
        chargingUnits.add(unit);
    }

    public void enable() {
        this.status = StationStatus.ACTIVE;
    }

    public void disable() {
        this.status = StationStatus.DISABLED;
    }

    public boolean isActive() {
        return status == StationStatus.ACTIVE;
    }

    public long countUnitsInStatus(ChargerStatus chargerStatus) {
        return chargingUnits.stream().filter(unit -> unit.getStatus() == chargerStatus).count();
    }

    public long countAvailableUnits() {
        return isActive() ? countUnitsInStatus(ChargerStatus.AVAILABLE) : 0;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public String getCity() {
        return city;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public LocalTime getOpeningTime() {
        return openingTime;
    }

    public LocalTime getClosingTime() {
        return closingTime;
    }

    public boolean isOpen24Hours() {
        return open24Hours;
    }

    public StationStatus getStatus() {
        return status;
    }

    public List<ChargingUnit> getChargingUnits() {
        return Collections.unmodifiableList(chargingUnits);
    }
}
