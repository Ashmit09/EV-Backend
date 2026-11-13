package com.voltx.enums;

import java.util.EnumSet;
import java.util.Set;

public enum ReservationStatus {
    /** Booked, waiting for the customer to start charging. */
    CONFIRMED,
    /** A charging session is running for this booking. */
    ACTIVE,
    COMPLETED,
    CANCELLED,
    /** The slot ended without the customer starting a session (no-show). */
    EXPIRED;

    /** Statuses that still occupy the time slot on the charger. */
    public static final Set<ReservationStatus> SLOT_HOLDING = EnumSet.of(CONFIRMED, ACTIVE);

    public boolean holdsSlot() {
        return SLOT_HOLDING.contains(this);
    }
}
