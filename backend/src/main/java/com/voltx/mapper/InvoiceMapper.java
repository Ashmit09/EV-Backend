package com.voltx.mapper;

import com.voltx.dto.response.InvoiceResponse;
import com.voltx.dto.response.PaymentResponse;
import com.voltx.entity.ChargingSession;
import com.voltx.entity.ChargingUnit;
import com.voltx.entity.Invoice;
import com.voltx.entity.Payment;

import java.util.List;

public final class InvoiceMapper {

    private InvoiceMapper() {
    }

    public static InvoiceResponse toResponse(Invoice invoice, List<Payment> payments, boolean includeCustomer) {
        ChargingSession session = invoice.getSession();
        ChargingUnit unit = session.getChargingUnit();
        return new InvoiceResponse(invoice.getId(), invoice.getInvoiceNumber(), invoice.getStatus(),
                invoice.getIssuedAt(), invoice.getPaidAt(), session.getId(), unit.getStation().getName(),
                unit.getStation().getAddress(), unit.getStation().getCity(), unit.getChargerCode(),
                unit.getConnectorType().getLabel(), session.getVehicle().getDisplayName(),
                session.getVehicle().getRegistrationNumber(), session.getStartTime(), session.getEndTime(),
                session.getSimulatedDurationMinutes(), session.getInitialBatteryPercentage(),
                session.getFinalBatteryPercentage(), invoice.getEnergyKwh(), invoice.getRatePerKwh(),
                invoice.getPricingPlan(), invoice.getPricingPlan().getLabel(), invoice.getEnergyCost(),
                invoice.getAdjustmentLabel(), invoice.getAdjustmentAmount(), invoice.getSubtotal(),
                invoice.getTaxRate(), invoice.getTaxAmount(), invoice.getTotalAmount(),
                includeCustomer ? invoice.getUser().getFullName() : null,
                includeCustomer ? invoice.getUser().getEmail() : null,
                payments.stream().map(InvoiceMapper::toPaymentResponse).toList());
    }

    public static PaymentResponse toPaymentResponse(Payment payment) {
        Invoice invoice = payment.getInvoice();
        return new PaymentResponse(payment.getId(), invoice.getId(), invoice.getInvoiceNumber(),
                invoice.getUser().getEmail(), payment.getAmount(), payment.getMethod(), payment.getStatus(),
                payment.getTransactionRef(), payment.getMessage(), payment.getProcessedAt());
    }
}
