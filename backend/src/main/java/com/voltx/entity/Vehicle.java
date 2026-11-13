package com.voltx.entity;

import com.voltx.enums.ConnectorType;
import com.voltx.enums.VehicleType;
import com.voltx.exception.BusinessRuleViolationException;
import com.voltx.exception.IncompatibleConnectorException;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Abstract parent of every electric vehicle.
 *
 * <h3>OOP in this class</h3>
 * <ul>
 *   <li><b>Abstraction</b> – {@code Vehicle} cannot be instantiated; it describes what every EV has
 *       and leaves type-specific rules to {@link #validateTypeSpecificConstraints(ChargingUnit)}.</li>
 *   <li><b>Inheritance</b> – {@link ElectricCar}, {@link ElectricBike} and {@link ElectricScooter}
 *       inherit all fields and the compatibility algorithm.</li>
 *   <li><b>Polymorphism</b> – services call {@code vehicle.checkCompatibility(unit)} on a
 *       {@code Vehicle} reference; the JVM runs the subclass's override at runtime.</li>
 *   <li><b>Encapsulation</b> – fields are private; the battery level can only change through
 *       {@link #updateBatteryLevel(int)}, which validates the range.</li>
 * </ul>
 * Persisted with SINGLE_TABLE inheritance: one {@code vehicles} table, {@code vehicle_type} column
 * as discriminator.
 */
@Entity
@Table(name = "vehicles")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "vehicle_type", discriminatorType = DiscriminatorType.STRING, length = 20)
public abstract class Vehicle extends BaseEntity {

    /** Power above which a charger is considered "ultra-fast". */
    protected static final BigDecimal ULTRA_FAST_THRESHOLD_KW = new BigDecimal("60");

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 60)
    private String manufacturer;

    @Column(nullable = false, length = 60)
    private String model;

    @Column(name = "registration_number", nullable = false, unique = true, length = 20)
    private String registrationNumber;

    @Column(name = "battery_capacity_kwh", nullable = false, precision = 6, scale = 2)
    private BigDecimal batteryCapacityKwh;

    @Column(name = "max_charging_power_kw", nullable = false, precision = 6, scale = 2)
    private BigDecimal maxChargingPowerKw;

    @Column(name = "current_battery_percentage", nullable = false)
    private int currentBatteryPercentage;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "vehicle_connectors", joinColumns = @JoinColumn(name = "vehicle_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "connector_type", nullable = false, length = 20)
    private Set<ConnectorType> supportedConnectors = new HashSet<>();

    protected Vehicle() {
        // required by JPA
    }

    protected Vehicle(User owner, String manufacturer, String model, String registrationNumber,
                      BigDecimal batteryCapacityKwh, BigDecimal maxChargingPowerKw,
                      int currentBatteryPercentage, Collection<ConnectorType> supportedConnectors) {
        this.owner = Objects.requireNonNull(owner, "Owner is required");
        this.registrationNumber = normalizeRegistration(registrationNumber);
        updateDetails(manufacturer, model, batteryCapacityKwh, maxChargingPowerKw, supportedConnectors);
        updateBatteryLevel(currentBatteryPercentage);
    }

    // ------------------------------------------------------------------ abstract contract

    /** Discriminator value exposed to the API. Overridden by every subclass. */
    public abstract VehicleType getVehicleType();

    /** Realistic battery range for this kind of vehicle, used to reject impossible specs. */
    protected abstract BigDecimal minBatteryCapacityKwh();

    protected abstract BigDecimal maxBatteryCapacityKwh();

    /** Highest on-board charging power this kind of vehicle can accept. */
    protected abstract BigDecimal maxSupportedChargingPowerKw();

    /**
     * Hook for rules that only apply to one vehicle type
     * (e.g. scooters cannot use DC chargers). Throws {@link IncompatibleConnectorException}.
     */
    protected abstract void validateTypeSpecificConstraints(ChargingUnit unit);

    // ------------------------------------------------------------------ behaviour

    /**
     * Template method: the common connector check runs first, then the subclass-specific rule.
     * Subclasses override only {@link #validateTypeSpecificConstraints}, never this method.
     * (It is deliberately not {@code final}: Hibernate lazy-loading proxies cannot intercept
     * final methods, which would make the check run against an uninitialised proxy.)
     */
    public void checkCompatibility(ChargingUnit unit) {
        Objects.requireNonNull(unit, "Charging unit is required");
        if (!supportedConnectors.contains(unit.getConnectorType())) {
            throw new IncompatibleConnectorException(String.format(
                    "%s supports %s but charger %s uses %s",
                    getDisplayName(), describeConnectors(), unit.getChargerCode(),
                    unit.getConnectorType().getLabel()));
        }
        validateTypeSpecificConstraints(unit);
    }

    /** Non-throwing variant used to annotate charger lists in the UI. */
    public Optional<String> findCompatibilityIssue(ChargingUnit unit) {
        try {
            checkCompatibility(unit);
            return Optional.empty();
        } catch (IncompatibleConnectorException ex) {
            return Optional.of(ex.getMessage());
        }
    }

    /** The real charging power is limited by the weaker side: charger or vehicle. */
    public BigDecimal effectiveChargingPower(BigDecimal chargerPowerKw) {
        return chargerPowerKw.min(maxChargingPowerKw);
    }

    /**
     * Default battery percentage to stop at. Overridden by {@link ElectricCar}, which stops DC
     * fast-charging at 80% to protect battery health.
     */
    public int defaultTargetPercentage(ChargingUnit unit) {
        return 100;
    }

    public void updateBatteryLevel(int percentage) {
        if (percentage < 0 || percentage > 100) {
            throw new BusinessRuleViolationException("Battery percentage must be between 0 and 100");
        }
        this.currentBatteryPercentage = percentage;
    }

    public void updateDetails(String manufacturer, String model, BigDecimal batteryCapacityKwh,
                              BigDecimal maxChargingPowerKw, Collection<ConnectorType> connectors) {
        this.manufacturer = requireText(manufacturer, "Manufacturer");
        this.model = requireText(model, "Model");
        this.batteryCapacityKwh = validateCapacity(batteryCapacityKwh);
        this.maxChargingPowerKw = validatePower(maxChargingPowerKw);
        if (connectors == null || connectors.isEmpty()) {
            throw new BusinessRuleViolationException("A vehicle must support at least one connector type");
        }
        this.supportedConnectors.clear();
        this.supportedConnectors.addAll(EnumSet.copyOf(connectors));
    }

    public boolean isOwnedBy(Long userId) {
        return owner.getId() != null && owner.getId().equals(userId);
    }

    public boolean supports(ConnectorType connectorType) {
        return supportedConnectors.contains(connectorType);
    }

    public String getDisplayName() {
        return manufacturer + " " + model;
    }

    // ------------------------------------------------------------------ validation helpers

    private BigDecimal validateCapacity(BigDecimal capacity) {
        if (capacity == null
                || capacity.compareTo(minBatteryCapacityKwh()) < 0
                || capacity.compareTo(maxBatteryCapacityKwh()) > 0) {
            throw new BusinessRuleViolationException(String.format(
                    "Battery capacity for a %s must be between %s and %s kWh",
                    getVehicleType().name().toLowerCase(Locale.ROOT),
                    minBatteryCapacityKwh().toPlainString(), maxBatteryCapacityKwh().toPlainString()));
        }
        return capacity;
    }

    private BigDecimal validatePower(BigDecimal power) {
        if (power == null || power.signum() <= 0 || power.compareTo(maxSupportedChargingPowerKw()) > 0) {
            throw new BusinessRuleViolationException(String.format(
                    "Maximum charging power for a %s must be between 0 and %s kW",
                    getVehicleType().name().toLowerCase(Locale.ROOT),
                    maxSupportedChargingPowerKw().toPlainString()));
        }
        return power;
    }

    private String describeConnectors() {
        return supportedConnectors.stream()
                .sorted()
                .map(ConnectorType::getLabel)
                .reduce((a, b) -> a + ", " + b)
                .orElse("no connectors");
    }

    private static String normalizeRegistration(String registration) {
        return requireText(registration, "Registration number")
                .replaceAll("[\\s-]", "")
                .toUpperCase(Locale.ROOT);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleViolationException(field + " must not be blank");
        }
        return value.trim();
    }

    // ------------------------------------------------------------------ getters only

    public User getOwner() {
        return owner;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public String getModel() {
        return model;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public BigDecimal getBatteryCapacityKwh() {
        return batteryCapacityKwh;
    }

    public BigDecimal getMaxChargingPowerKw() {
        return maxChargingPowerKw;
    }

    public int getCurrentBatteryPercentage() {
        return currentBatteryPercentage;
    }

    public Set<ConnectorType> getSupportedConnectors() {
        return Collections.unmodifiableSet(supportedConnectors);
    }
}
