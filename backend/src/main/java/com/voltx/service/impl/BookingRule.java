package com.voltx.service.impl;

/**
 * Functional interface: one booking rule. Rules are written as lambdas / method references
 * in {@link BookingPolicy} and evaluated in order; a rule signals a violation by throwing.
 */
@FunctionalInterface
public interface BookingRule {

    void check(BookingContext context);
}
