package com.voltx.exception;

import org.springframework.http.HttpStatus;

/** The simulated payment processor declined the payment. */
public class PaymentFailedException extends VoltXException {

    public PaymentFailedException(String message) {
        super(message);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.PAYMENT_REQUIRED;
    }

    @Override
    public String getErrorCode() {
        return "PAYMENT_DECLINED";
    }
}
