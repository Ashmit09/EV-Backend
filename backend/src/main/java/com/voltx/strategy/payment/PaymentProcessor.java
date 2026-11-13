package com.voltx.strategy.payment;

import com.voltx.entity.Invoice;
import com.voltx.enums.PaymentMethod;

/**
 * Abstraction over a payment channel. VoltX has no real gateway; implementations simulate
 * authorisation with simple, explainable rules and return a transaction reference.
 */
public interface PaymentProcessor {

    PaymentMethod method();

    PaymentResult process(Invoice invoice);
}
