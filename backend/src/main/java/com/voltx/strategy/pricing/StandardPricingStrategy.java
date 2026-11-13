package com.voltx.strategy.pricing;

import com.voltx.enums.PricingPlan;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Flat tariff: cost = energy × rate. */
@Component
public class StandardPricingStrategy implements PricingStrategy {

    @Override
    public PricingPlan plan() {
        return PricingPlan.STANDARD;
    }

    @Override
    public PriceBreakdown calculatePrice(BigDecimal energyKwh, BigDecimal ratePerKwh, LocalDateTime sessionStart) {
        BigDecimal energyCost = baseEnergyCost(energyKwh, ratePerKwh);
        return new PriceBreakdown(plan(), energyKwh, ratePerKwh, energyCost, null, BigDecimal.ZERO.setScale(2), energyCost);
    }
}
