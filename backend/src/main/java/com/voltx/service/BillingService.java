package com.voltx.service;

import com.voltx.dto.response.InvoiceResponse;
import com.voltx.dto.response.PaymentResponse;
import com.voltx.entity.ChargingSession;
import com.voltx.entity.Invoice;
import com.voltx.enums.PaymentMethod;
import com.voltx.strategy.pricing.PriceBreakdown;

import java.util.List;

public interface BillingService {

    Invoice generateInvoice(ChargingSession session, PriceBreakdown breakdown);

    List<InvoiceResponse> myInvoices(Long userId);

    InvoiceResponse get(Long userId, Long invoiceId);

    InvoiceResponse pay(Long userId, Long invoiceId, PaymentMethod method);

    List<InvoiceResponse> listAll();

    List<PaymentResponse> listPayments();
}
