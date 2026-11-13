package com.voltx.dto.response;

import com.voltx.enums.PaymentMethod;
import com.voltx.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(Long id, Long invoiceId, String invoiceNumber, String customerEmail,
                              BigDecimal amount, PaymentMethod method, PaymentStatus status,
                              String transactionRef, String message, LocalDateTime processedAt) {
}
