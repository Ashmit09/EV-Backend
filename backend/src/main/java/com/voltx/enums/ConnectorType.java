package com.voltx.enums;

/**
 * Charging connector standards used in India.
 * Each constant carries its display label and whether it delivers DC (fast) power.
 */
public enum ConnectorType {
    CCS2("CCS2", true),
    TYPE_2("Type 2", false),
    CHADEMO("CHAdeMO", true),
    GBT("GB/T", true);

    private final String label;
    private final boolean directCurrent;

    ConnectorType(String label, boolean directCurrent) {
        this.label = label;
        this.directCurrent = directCurrent;
    }

    public String getLabel() {
        return label;
    }

    public boolean isDirectCurrent() {
        return directCurrent;
    }
}
