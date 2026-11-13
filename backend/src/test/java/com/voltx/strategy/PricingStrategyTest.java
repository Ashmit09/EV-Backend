package com.voltx.strategy;

import com.voltx.enums.PricingPlan;
import com.voltx.strategy.pricing.PeakHourPricingStrategy;
import com.voltx.strategy.pricing.PriceBreakdown;
import com.voltx.strategy.pricing.PricingStrategy;
import com.voltx.strategy.pricing.PromotionalPricingStrategy;
import com.voltx.strategy.pricing.StandardPricingStrategy;
import com.voltx.util.StrategyRegistry;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Runtime polymorphism through the PricingStrategy interface. */
class PricingStrategyTest {

    private static final BigDecimal ENERGY = new BigDecimal("10.000");
    private static final BigDecimal RATE = new BigDecimal("20.00");
    private static final LocalDateTime MORNING = LocalDateTime.of(2026, 11, 13, 9, 0);
    private static final LocalDateTime EVENING = LocalDateTime.of(2026, 11, 13, 19, 30);

    private final StrategyRegistry<PricingPlan, PricingStrategy> registry = new StrategyRegistry<>(PricingPlan.class,
            List.of(new StandardPricingStrategy(), new PeakHourPricingStrategy(), new PromotionalPricingStrategy()),
            PricingStrategy::plan, "pricing strategy");

    @Test
    void standardIsEnergyTimesRate() {
        PriceBreakdown price = registry.get(PricingPlan.STANDARD).calculatePrice(ENERGY, RATE, EVENING);
        assertThat(price.subtotal()).isEqualByComparingTo("200.00");
        assertThat(price.adjustmentAmount()).isEqualByComparingTo("0");
    }

    @Test
    void peakHourAddsSurchargeOnlyInTheEvening() {
        PricingStrategy peak = registry.get(PricingPlan.PEAK_HOUR);
        assertThat(peak.calculatePrice(ENERGY, RATE, EVENING).subtotal()).isEqualByComparingTo("250.00");
        assertThat(peak.calculatePrice(ENERGY, RATE, MORNING).subtotal()).isEqualByComparingTo("200.00");
        assertThat(peak.calculatePrice(ENERGY, RATE, LocalDateTime.of(2026, 11, 13, 22, 0)).subtotal())
                .isEqualByComparingTo("200.00"); // 22:00 is the exclusive end of the peak window
    }

    @Test
    void promotionalGivesFifteenPercentOff() {
        PriceBreakdown price = registry.get(PricingPlan.PROMOTIONAL).calculatePrice(ENERGY, RATE, MORNING);
        assertThat(price.adjustmentAmount()).isEqualByComparingTo("-30.00");
        assertThat(price.subtotal()).isEqualByComparingTo("170.00");
    }

    @Test
    void registryResolvesEveryPlan() {
        assertThat(registry.keys()).containsExactlyInAnyOrder(PricingPlan.values());
    }
}
