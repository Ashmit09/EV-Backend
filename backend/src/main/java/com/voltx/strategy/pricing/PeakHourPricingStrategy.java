package com.voltx.strategy.pricing;

import com.voltx.enums.PricingPlan;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Time-of-day tariff: sessions that start in the evening grid peak (18:00–22:00)
 * pay a 25% surcharge; all other sessions pay the base rate.
 */
@Component
public class PeakHourPricingStrategy implements PricingStrategy {

    static final LocalTime PEAK_START = LocalTime.of(18, 0);
    static final LocalTime PEAK_END = LocalTime.of(22, 0);
    static final BigDecimal SURCHARGE_RATE = new BigDecimal("0.25");

    @Override
    public PricingPlan plan() {
        return PricingPlan.PEAK_HOUR;
    }

    @Override
    public PriceBreakdown calculatePrice(BigDecimal energyKwh, BigDecimal ratePerKwh, LocalDateTime sessionStart) {
        BigDecimal energyCost = baseEnergyCost(energyKwh, ratePerKwh);
        if (!isPeak(sessionStart.toLocalTime())) {
            return new PriceBreakdown(plan(), energyKwh, ratePerKwh, energyCost,
                    "Off-peak (no surcharge)", BigDecimal.ZERO.setScale(2), energyCost);
        }
        BigDecimal surcharge = energyCost.multiply(SURCHARGE_RATE).setScale(2, RoundingMode.HALF_UP);
        return new PriceBreakdown(plan(), energyKwh, ratePerKwh, energyCost,
                "Peak-hour surcharge (25%)", surcharge, energyCost.add(surcharge));
    }

    public boolean isPeak(LocalTime time) {
        return !time.isBefore(PEAK_START) && time.isBefore(PEAK_END);
    }
}
