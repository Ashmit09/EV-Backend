package com.voltx.enums;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Operational state of a charging unit together with the table of legal transitions.
 *
 * <pre>
 * AVAILABLE ──reserve──▶ RESERVED ──start──▶ CHARGING ──complete──▶ AVAILABLE
 *     │                     └──release──▶ AVAILABLE
 *     ├──▶ OUT_OF_SERVICE ◀──▶ MAINTENANCE
 *     └──────────── restore ◀──────┘
 * </pre>
 *
 * Keeping the table here (instead of in if/else chains in services) means there is exactly
 * one place that decides whether a transition is valid.
 */
public enum ChargerStatus {
    AVAILABLE,
    RESERVED,
    CHARGING,
    OUT_OF_SERVICE,
    MAINTENANCE;

    private static final Map<ChargerStatus, Set<ChargerStatus>> ALLOWED = new EnumMap<>(ChargerStatus.class);

    static {
        ALLOWED.put(AVAILABLE, EnumSet.of(RESERVED, OUT_OF_SERVICE, MAINTENANCE));
        ALLOWED.put(RESERVED, EnumSet.of(CHARGING, AVAILABLE));
        ALLOWED.put(CHARGING, EnumSet.of(AVAILABLE));
        ALLOWED.put(OUT_OF_SERVICE, EnumSet.of(AVAILABLE, MAINTENANCE));
        ALLOWED.put(MAINTENANCE, EnumSet.of(AVAILABLE, OUT_OF_SERVICE));
    }

    public boolean canTransitionTo(ChargerStatus target) {
        return ALLOWED.get(this).contains(target);
    }

    public Set<ChargerStatus> allowedTransitions() {
        return Collections.unmodifiableSet(ALLOWED.get(this));
    }

    /** A charger that is out of service or under maintenance cannot accept bookings. */
    public boolean acceptsBookings() {
        return this == AVAILABLE || this == RESERVED || this == CHARGING;
    }
}
