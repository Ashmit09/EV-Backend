package com.voltx.strategy.pricing;

import com.voltx.enums.PricingPlan;

import java.math.BigDecimal;

/**
 * Immutable result of a pricing calculation (Java record ⇒ private final fields, no setters).
 *
 * @param energyCost       energy × rate, before any plan-specific adjustment
 * @param adjustmentAmount positive for a surcharge, negative for a discount, zero otherwise
 * @param subtotal         energyCost + adjustmentAmount (pre-tax charging cost)
 */
public record PriceBreakdown(
        PricingPlan plan,
        BigDecimal energyKwh,
        BigDecimal ratePerKwh,
        BigDecimal energyCost,
        String adjustmentLabel,
        BigDecimal adjustmentAmount,
        BigDecimal subtotal) {
}
