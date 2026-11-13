package com.voltx.strategy.payment;

import com.voltx.enums.PaymentMethod;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** VoltX wallet: prepaid wallets in India are capped, simulated here at ₹2,000 per payment. */
@Component
public class WalletPaymentProcessor extends AbstractPaymentProcessor {

    static final BigDecimal WALLET_LIMIT = new BigDecimal("2000");

    @Override
    public PaymentMethod method() {
        return PaymentMethod.WALLET;
    }

    @Override
    protected String referencePrefix() {
        return "WAL";
    }

    @Override
    protected boolean authorize(BigDecimal amount) {
        return amount.compareTo(WALLET_LIMIT) <= 0;
    }

    @Override
    protected String declineReason(BigDecimal amount) {
        return "Wallet payments are limited to ₹2,000; please use UPI or card";
    }
}
