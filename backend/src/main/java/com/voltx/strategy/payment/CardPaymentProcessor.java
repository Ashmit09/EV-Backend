package com.voltx.strategy.payment;

import com.voltx.enums.PaymentMethod;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Debit/credit card: approved up to a simulated card limit of ₹2,00,000. */
@Component
public class CardPaymentProcessor extends AbstractPaymentProcessor {

    static final BigDecimal CARD_LIMIT = new BigDecimal("200000");

    @Override
    public PaymentMethod method() {
        return PaymentMethod.CARD;
    }

    @Override
    protected String referencePrefix() {
        return "CRD";
    }

    @Override
    protected boolean authorize(BigDecimal amount) {
        return amount.compareTo(CARD_LIMIT) <= 0;
    }
}
