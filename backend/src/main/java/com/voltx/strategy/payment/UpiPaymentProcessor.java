package com.voltx.strategy.payment;

import com.voltx.enums.PaymentMethod;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** UPI: approved up to the NPCI per-transaction limit of ₹1,00,000. */
@Component
public class UpiPaymentProcessor extends AbstractPaymentProcessor {

    static final BigDecimal UPI_LIMIT = new BigDecimal("100000");

    @Override
    public PaymentMethod method() {
        return PaymentMethod.UPI;
    }

    @Override
    protected String referencePrefix() {
        return "UPI";
    }

    @Override
    protected boolean authorize(BigDecimal amount) {
        return amount.compareTo(UPI_LIMIT) <= 0;
    }

    @Override
    protected String declineReason(BigDecimal amount) {
        return "UPI transactions are limited to ₹1,00,000";
    }
}
