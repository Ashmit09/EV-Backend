package com.voltx.util;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Generic lookup table from an enum key to the strategy implementing it.
 * <p>
 * Generics: the same class serves {@code StrategyRegistry<PricingPlan, PricingStrategy>} and
 * {@code StrategyRegistry<PaymentMethod, PaymentProcessor>} with full compile-time type safety.
 *
 * @param <K> enum key type
 * @param <S> strategy type
 */
public final class StrategyRegistry<K extends Enum<K>, S> {

    private final Map<K, S> strategies;
    private final String description;

    public StrategyRegistry(Class<K> keyType, Collection<? extends S> implementations,
                            Function<? super S, K> keyExtractor, String description) {
        this.description = description;
        this.strategies = Collections.unmodifiableMap(implementations.stream()
                .collect(Collectors.toMap(
                        keyExtractor,
                        Function.identity(),
                        (first, second) -> {
                            throw new IllegalStateException("Two " + description + " registered for the same key");
                        },
                        () -> new EnumMap<>(keyType))));
    }

    public S get(K key) {
        S strategy = strategies.get(key);
        if (strategy == null) {
            throw new IllegalStateException("No " + description + " registered for " + key);
        }
        return strategy;
    }

    public Set<K> keys() {
        return strategies.keySet();
    }
}
