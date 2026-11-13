package com.voltx.entity;

import com.voltx.enums.PaymentMethod;
import com.voltx.enums.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/** Immutable record of one (simulated) payment attempt against an invoice. */
@Entity
@Table(name = "payments")
public class Payment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "transaction_ref", nullable = false, unique = true, length = 40)
    private String transactionRef;

    @Column(length = 255)
    private String message;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    protected Payment() {
        // required by JPA
    }

    public Payment(Invoice invoice, PaymentMethod method, PaymentStatus status, String transactionRef,
                   String message, LocalDateTime processedAt) {
        this.invoice = Objects.requireNonNull(invoice);
        this.amount = invoice.getTotalAmount();
        this.method = Objects.requireNonNull(method);
        this.status = Objects.requireNonNull(status);
        this.transactionRef = Objects.requireNonNull(transactionRef);
        this.message = message;
        this.processedAt = Objects.requireNonNull(processedAt);
    }

    public boolean isSuccessful() {
        return status == PaymentStatus.SUCCESS;
    }

    public Invoice getInvoice() {
        return invoice;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getTransactionRef() {
        return transactionRef;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }
}
