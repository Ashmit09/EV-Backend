package com.voltx.config;

import com.voltx.enums.PaymentMethod;
import com.voltx.enums.PricingPlan;
import com.voltx.strategy.payment.PaymentProcessor;
import com.voltx.strategy.pricing.PricingStrategy;
import com.voltx.util.StrategyRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.Clock;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AppConfig {

    /**
     * Services read the time from this Clock instead of calling LocalDateTime.now() directly,
     * so tests can freeze time (e.g. to test peak-hour pricing at 19:00).
     */
    @Bean
    public Clock clock(VoltXProperties properties) {
        return Clock.system(ZoneId.of(properties.timezone()));
    }

    /** Every PricingStrategy bean found by Spring, indexed by its plan. */
    @Bean
    public StrategyRegistry<PricingPlan, PricingStrategy> pricingStrategies(List<PricingStrategy> strategies) {
        return new StrategyRegistry<>(PricingPlan.class, strategies, PricingStrategy::plan, "pricing strategy");
    }

    @Bean
    public StrategyRegistry<PaymentMethod, PaymentProcessor> paymentProcessors(List<PaymentProcessor> processors) {
        return new StrategyRegistry<>(PaymentMethod.class, processors, PaymentProcessor::method, "payment processor");
    }

    /** Small pool for fire-and-forget work (notifications) so API requests are never blocked by it. */
    @Bean(name = "notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("voltx-notify-");
        executor.initialize();
        return executor;
    }
}
