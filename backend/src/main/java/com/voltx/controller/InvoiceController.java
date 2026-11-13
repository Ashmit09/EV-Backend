package com.voltx.controller;

import com.voltx.dto.request.PaymentRequest;
import com.voltx.dto.response.InvoiceResponse;
import com.voltx.security.VoltXUserDetails;
import com.voltx.service.BillingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/invoices")
@Tag(name = "Billing")
public class InvoiceController {

    private final BillingService billingService;

    public InvoiceController(BillingService billingService) {
        this.billingService = billingService;
    }

    @GetMapping
    public List<InvoiceResponse> mine(@AuthenticationPrincipal VoltXUserDetails me) {
        return billingService.myInvoices(me.getId());
    }

    @GetMapping("/{id}")
    public InvoiceResponse get(@AuthenticationPrincipal VoltXUserDetails me, @PathVariable Long id) {
        return billingService.get(me.getId(), id);
    }

    @PostMapping("/{id}/pay")
    @Operation(summary = "Pay an invoice with a simulated UPI / card / wallet processor (402 if declined)")
    public InvoiceResponse pay(@AuthenticationPrincipal VoltXUserDetails me, @PathVariable Long id,
                               @Valid @RequestBody PaymentRequest request) {
        return billingService.pay(me.getId(), id, request.method());
    }
}
