package com.voltx.strategy.pricing;

import com.voltx.enums.PricingPlan;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/** Promotional tariff: a flat 15% green-energy discount on every session. */
@Component
public class PromotionalPricingStrategy implements PricingStrategy {

    static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.15");

    @Override
    public PricingPlan plan() {
        return PricingPlan.PROMOTIONAL;
    }

    @Override
    public PriceBreakdown calculatePrice(BigDecimal energyKwh, BigDecimal ratePerKwh, LocalDateTime sessionStart) {
        BigDecimal energyCost = baseEnergyCost(energyKwh, ratePerKwh);
        BigDecimal discount = energyCost.multiply(DISCOUNT_RATE).setScale(2, RoundingMode.HALF_UP).negate();
        return new PriceBreakdown(plan(), energyKwh, ratePerKwh, energyCost,
                "Green promo discount (15%)", discount, energyCost.add(discount));
    }
}
