package com.voltx.enums;

/** Pricing models an administrator can assign to a charging unit. */
public enum PricingPlan {
    STANDARD("Standard", "Flat rate per kWh at all hours"),
    PEAK_HOUR("Peak hour", "25% surcharge for sessions starting 18:00-22:00"),
    PROMOTIONAL("Promotional", "15% green-energy discount on every session");

    private final String label;
    private final String description;

    PricingPlan(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }
}
