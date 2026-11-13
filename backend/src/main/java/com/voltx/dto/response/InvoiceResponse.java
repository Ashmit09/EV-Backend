package com.voltx.dto.response;

import com.voltx.enums.InvoiceStatus;
import com.voltx.enums.PricingPlan;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record InvoiceResponse(Long id, String invoiceNumber, InvoiceStatus status, LocalDateTime issuedAt,
                              LocalDateTime paidAt, Long sessionId, String stationName, String stationAddress,
                              String city, String chargerCode, String connectorLabel, String vehicleName,
                              String registrationNumber, LocalDateTime sessionStart, LocalDateTime sessionEnd,
                              Integer simulatedDurationMinutes, Double initialBatteryPercentage,
                              Double finalBatteryPercentage, BigDecimal energyKwh, BigDecimal ratePerKwh,
                              PricingPlan pricingPlan, String pricingPlanLabel, BigDecimal energyCost,
                              String adjustmentLabel, BigDecimal adjustmentAmount, BigDecimal subtotal,
                              BigDecimal taxRate, BigDecimal taxAmount, BigDecimal totalAmount,
                              String customerName, String customerEmail, List<PaymentResponse> payments) {
}
