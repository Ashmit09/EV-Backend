package com.voltx.service.impl;

import com.voltx.config.VoltXProperties;
import com.voltx.dto.response.InvoiceResponse;
import com.voltx.dto.response.PaymentResponse;
import com.voltx.entity.ChargingSession;
import com.voltx.entity.Invoice;
import com.voltx.entity.Payment;
import com.voltx.enums.AuditAction;
import com.voltx.enums.NotificationChannel;
import com.voltx.enums.PaymentMethod;
import com.voltx.enums.PaymentStatus;
import com.voltx.exception.InvoiceNotFoundException;
import com.voltx.exception.PaymentFailedException;
import com.voltx.exception.UnauthorizedOperationException;
import com.voltx.mapper.InvoiceMapper;
import com.voltx.repository.InvoiceRepository;
import com.voltx.repository.PaymentRepository;
import com.voltx.service.AuditService;
import com.voltx.service.BillingService;
import com.voltx.service.NotificationService;
import com.voltx.strategy.payment.PaymentProcessor;
import com.voltx.strategy.payment.PaymentResult;
import com.voltx.strategy.pricing.PriceBreakdown;
import com.voltx.util.StrategyRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class BillingServiceImpl implements BillingService {

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final StrategyRegistry<PaymentMethod, PaymentProcessor> paymentProcessors;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final VoltXProperties properties;
    private final Clock clock;

    public BillingServiceImpl(InvoiceRepository invoiceRepository, PaymentRepository paymentRepository,
                              StrategyRegistry<PaymentMethod, PaymentProcessor> paymentProcessors,
                              AuditService auditService, NotificationService notificationService,
                              VoltXProperties properties, Clock clock) {
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.paymentProcessors = paymentProcessors;
        this.auditService = auditService;
        this.notificationService = notificationService;
        this.properties = properties;
        this.clock = clock;
    }

    /** Runs inside the session-completion transaction (propagation REQUIRED). */
    @Override
    @Transactional
    public Invoice generateInvoice(ChargingSession session, PriceBreakdown breakdown) {
        LocalDateTime now = LocalDateTime.now(clock);
        Invoice invoice = Invoice.issue(session, breakdown, properties.billing().gstRate(), now);
        if (invoice.getTotalAmount().signum() == 0) {
            invoice.markPaid(now); // stopped before any energy flowed: nothing to pay
        }
        invoice = invoiceRepository.save(invoice);
        auditService.record(session.getUser().getEmail(), AuditAction.INVOICE_GENERATED, "Invoice", invoice.getId(),
                invoice.getInvoiceNumber() + " total ₹" + invoice.getTotalAmount().toPlainString());
        return invoice;
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceResponse> myInvoices(Long userId) {
        return invoiceRepository.findByUserIdWithDetails(userId).stream()
                .map(invoice -> InvoiceMapper.toResponse(invoice, List.of(), false))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponse get(Long userId, Long invoiceId) {
        Invoice invoice = findOwned(userId, invoiceId);
        return InvoiceMapper.toResponse(invoice, paymentRepository.findByInvoiceIdOrderByProcessedAtDesc(invoiceId), false);
    }

    /**
     * Runtime polymorphism: {@code processor} is a {@link PaymentProcessor} reference; UPI, card or
     * wallet logic runs depending on the method chosen. A declined payment is still recorded
     * ({@code noRollbackFor}), then reported to the client as 402.
     */
    @Override
    @Transactional(noRollbackFor = PaymentFailedException.class)
    public InvoiceResponse pay(Long userId, Long invoiceId, PaymentMethod method) {
        Invoice invoice = findOwned(userId, invoiceId);
        PaymentProcessor processor = paymentProcessors.get(method);
        PaymentResult result = processor.process(invoice);
        LocalDateTime now = LocalDateTime.now(clock);

        paymentRepository.save(new Payment(invoice, method,
                result.success() ? PaymentStatus.SUCCESS : PaymentStatus.FAILED,
                result.transactionRef(), result.message(), now));
        auditService.record(invoice.getUser().getEmail(), AuditAction.PAYMENT_PROCESSED, "Invoice", invoiceId,
                method + " " + (result.success() ? "SUCCESS" : "FAILED") + " " + result.transactionRef());
        if (!result.success()) {
            throw new PaymentFailedException(result.message());
        }
        invoice.markPaid(now);
        notificationService.notify(invoice.getUser(), "Payment received",
                "₹" + invoice.getTotalAmount().toPlainString() + " paid for " + invoice.getInvoiceNumber() + ".",
                NotificationChannel.IN_APP, NotificationChannel.EMAIL, NotificationChannel.SMS);
        return InvoiceMapper.toResponse(invoice, paymentRepository.findByInvoiceIdOrderByProcessedAtDesc(invoiceId), false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceResponse> listAll() {
        return invoiceRepository.findAllWithDetails().stream()
                .map(invoice -> InvoiceMapper.toResponse(invoice, List.of(), true))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> listPayments() {
        return paymentRepository.findAllWithInvoice().stream().map(InvoiceMapper::toPaymentResponse).toList();
    }

    private Invoice findOwned(Long userId, Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId).orElseThrow(() -> new InvoiceNotFoundException(invoiceId));
        if (!invoice.isOwnedBy(userId)) {
            throw new UnauthorizedOperationException("You can only access your own invoices");
        }
        return invoice;
    }
}
