package com.voltx.strategy;

import com.voltx.TestFixtures;
import com.voltx.entity.ChargingSession;
import com.voltx.entity.ChargingUnit;
import com.voltx.entity.Invoice;
import com.voltx.entity.Reservation;
import com.voltx.entity.User;
import com.voltx.entity.Vehicle;
import com.voltx.enums.ConnectorType;
import com.voltx.enums.PricingPlan;
import com.voltx.strategy.payment.PaymentProcessor;
import com.voltx.strategy.payment.PaymentResult;
import com.voltx.strategy.payment.UpiPaymentProcessor;
import com.voltx.strategy.payment.WalletPaymentProcessor;
import com.voltx.strategy.pricing.StandardPricingStrategy;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentProcessorTest {

    private Invoice invoiceFor(String energyKwh) {
        User user = TestFixtures.customer();
        Vehicle car = TestFixtures.nexon(user);
        ChargingUnit unit = TestFixtures.unit(TestFixtures.station24h(), "VX-P", ConnectorType.CCS2, "60", PricingPlan.STANDARD);
        LocalDateTime start = LocalDateTime.of(2026, 11, 1, 10, 0);
        Reservation reservation = new Reservation(user, car, unit, start, start.plusHours(1));
        reservation.activate();
        ChargingSession session = new ChargingSession(reservation, start, 10, 100, new BigDecimal("50"));
        var price = new StandardPricingStrategy().calculatePrice(new BigDecimal(energyKwh), unit.getPricePerKwh(), start);
        session.complete(start.plusMinutes(30), 50, new BigDecimal(energyKwh), 30, price.subtotal());
        return Invoice.issue(session, price, new BigDecimal("0.18"), start.plusMinutes(30));
    }

    @Test
    void walletDeclinesAboveItsLimitWhileUpiApproves() {
        Invoice large = invoiceFor("100"); // 100 kWh × ₹20 × 1.18 = ₹2,360
        PaymentProcessor wallet = new WalletPaymentProcessor();
        PaymentProcessor upi = new UpiPaymentProcessor();

        PaymentResult declined = wallet.process(large);
        assertThat(declined.success()).isFalse();
        assertThat(declined.message()).contains("₹2,000");

        PaymentResult approved = upi.process(large);
        assertThat(approved.success()).isTrue();
        assertThat(approved.transactionRef()).startsWith("UPI-");
    }

    @Test
    void walletApprovesSmallAmounts() {
        assertThat(new WalletPaymentProcessor().process(invoiceFor("5")).success()).isTrue();
    }
}
