package com.voltx.strategy.payment;

import com.voltx.entity.Invoice;
import com.voltx.exception.BusinessRuleViolationException;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.UUID;

/**
 * Abstract class vs interface, in one place:
 * <ul>
 *   <li>{@link PaymentProcessor} (interface) states <i>what</i> a processor can do.</li>
 *   <li>This abstract class holds the <i>shared</i> state-free algorithm every processor follows
 *       (template method): validate → authorise → build the transaction reference.</li>
 *   <li>Subclasses fill in only the parts that differ: {@link #referencePrefix()} and
 *       {@link #authorize(BigDecimal)}.</li>
 * </ul>
 */
public abstract class AbstractPaymentProcessor implements PaymentProcessor {

    @Override
    public final PaymentResult process(Invoice invoice) {
        if (invoice.isPaid()) {
            throw new BusinessRuleViolationException("Invoice " + invoice.getInvoiceNumber() + " is already paid");
        }
        BigDecimal amount = invoice.getTotalAmount();
        if (amount.signum() <= 0) {
            throw new BusinessRuleViolationException("Invoice amount must be positive");
        }
        String reference = referencePrefix() + "-"
                + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
        if (authorize(amount)) {
            return new PaymentResult(true, reference, method() + " payment of ₹" + amount.toPlainString() + " approved");
        }
        return new PaymentResult(false, reference, declineReason(amount));
    }

    protected abstract String referencePrefix();

    /** Simulated authorisation rule for this channel. */
    protected abstract boolean authorize(BigDecimal amount);

    /** Hook with a default implementation; subclasses may override with a precise reason. */
    protected String declineReason(BigDecimal amount) {
        return method() + " payment was declined";
    }
}
