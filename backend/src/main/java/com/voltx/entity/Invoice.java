package com.voltx.entity;

import com.voltx.enums.InvoiceStatus;
import com.voltx.enums.PricingPlan;
import com.voltx.exception.BusinessRuleViolationException;
import com.voltx.strategy.pricing.PriceBreakdown;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Bill for one completed charging session. All amounts are computed once, in the
 * {@link #issue} factory, from the pricing strategy's breakdown — they are never set from outside.
 */
@Entity
@Table(name = "invoices")
public class Invoice extends BaseEntity {

    @Column(name = "invoice_number", nullable = false, unique = true, length = 30)
    private String invoiceNumber;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false, unique = true)
    private ChargingSession session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "energy_kwh", nullable = false, precision = 8, scale = 3)
    private BigDecimal energyKwh;

    @Column(name = "rate_per_kwh", nullable = false, precision = 8, scale = 2)
    private BigDecimal ratePerKwh;

    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_plan", nullable = false, length = 20)
    private PricingPlan pricingPlan;

    @Column(name = "energy_cost", nullable = false, precision = 10, scale = 2)
    private BigDecimal energyCost;

    @Column(name = "adjustment_label", length = 80)
    private String adjustmentLabel;

    @Column(name = "adjustment_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal adjustmentAmount;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "tax_rate", nullable = false, precision = 4, scale = 3)
    private BigDecimal taxRate;

    @Column(name = "tax_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvoiceStatus status = InvoiceStatus.UNPAID;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    protected Invoice() {
        // required by JPA
    }

    private Invoice(String invoiceNumber, ChargingSession session, PriceBreakdown breakdown,
                    BigDecimal taxRate, LocalDateTime issuedAt) {
        this.invoiceNumber = invoiceNumber;
        this.session = session;
        this.user = session.getUser();
        this.energyKwh = breakdown.energyKwh();
        this.ratePerKwh = breakdown.ratePerKwh();
        this.pricingPlan = breakdown.plan();
        this.energyCost = breakdown.energyCost();
        this.adjustmentLabel = breakdown.adjustmentLabel();
        this.adjustmentAmount = breakdown.adjustmentAmount();
        this.subtotal = breakdown.subtotal();
        this.taxRate = taxRate;
        this.taxAmount = subtotal.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
        this.totalAmount = subtotal.add(taxAmount);
        this.issuedAt = issuedAt;
    }

    /** Factory method: the only way to create an invoice. */
    public static Invoice issue(ChargingSession session, PriceBreakdown breakdown, BigDecimal taxRate,
                                LocalDateTime issuedAt) {
        Objects.requireNonNull(session, "Session is required");
        if (session.isActive()) {
            throw new BusinessRuleViolationException("Cannot invoice a session that is still charging");
        }
        return new Invoice(numberFor(session, issuedAt), session, breakdown, taxRate, issuedAt);
    }

    private static String numberFor(ChargingSession session, LocalDateTime issuedAt) {
        return String.format("VX-%d%02d-%06d", issuedAt.getYear(), issuedAt.getMonthValue(), session.getId());
    }

    public void markPaid(LocalDateTime at) {
        if (status == InvoiceStatus.PAID) {
            throw new BusinessRuleViolationException("Invoice " + invoiceNumber + " is already paid");
        }
        this.status = InvoiceStatus.PAID;
        this.paidAt = at;
    }

    public boolean isPaid() {
        return status == InvoiceStatus.PAID;
    }

    public boolean isOwnedBy(Long userId) {
        return user.getId() != null && user.getId().equals(userId);
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public ChargingSession getSession() {
        return session;
    }

    public User getUser() {
        return user;
    }

    public BigDecimal getEnergyKwh() {
        return energyKwh;
    }

    public BigDecimal getRatePerKwh() {
        return ratePerKwh;
    }

    public PricingPlan getPricingPlan() {
        return pricingPlan;
    }

    public BigDecimal getEnergyCost() {
        return energyCost;
    }

    public String getAdjustmentLabel() {
        return adjustmentLabel;
    }

    public BigDecimal getAdjustmentAmount() {
        return adjustmentAmount;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public InvoiceStatus getStatus() {
        return status;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }
}
