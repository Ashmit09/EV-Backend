package com.voltx.strategy.pricing;

import com.voltx.enums.PricingPlan;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Strategy pattern + abstraction: billing code depends only on this interface.
 * <p>
 * Why an interface and not one class with a {@code switch}? Adding a new tariff (say, a
 * fleet discount) means writing one new class; {@code BillingServiceImpl} and the session
 * service never change. That is the Open/Closed principle.
 */
public interface PricingStrategy {

    /** Which pricing plan this strategy implements; used as the registry key. */
    PricingPlan plan();

    /** Runtime polymorphism: each implementation overrides this with its own rule. */
    PriceBreakdown calculatePrice(BigDecimal energyKwh, BigDecimal ratePerKwh, LocalDateTime sessionStart);

    /** Shared helper available to every implementation (Java 8+ default method). */
    default BigDecimal baseEnergyCost(BigDecimal energyKwh, BigDecimal ratePerKwh) {
        return energyKwh.multiply(ratePerKwh).setScale(2, RoundingMode.HALF_UP);
    }
}
