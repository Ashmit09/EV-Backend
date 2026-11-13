package com.voltx.strategy.payment;

public record PaymentResult(boolean success, String transactionRef, String message) {
}
