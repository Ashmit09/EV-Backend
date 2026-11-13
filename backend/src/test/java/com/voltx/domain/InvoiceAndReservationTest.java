package com.voltx.domain;

import com.voltx.TestFixtures;
import com.voltx.entity.ChargingSession;
import com.voltx.entity.ChargingUnit;
import com.voltx.entity.Invoice;
import com.voltx.entity.Reservation;
import com.voltx.entity.User;
import com.voltx.entity.Vehicle;
import com.voltx.enums.ConnectorType;
import com.voltx.enums.InvoiceStatus;
import com.voltx.enums.PricingPlan;
import com.voltx.enums.ReservationStatus;
import com.voltx.exception.BusinessRuleViolationException;
import com.voltx.exception.InvalidBookingException;
import com.voltx.exception.InvalidSessionException;
import com.voltx.strategy.pricing.PriceBreakdown;
import com.voltx.strategy.pricing.StandardPricingStrategy;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InvoiceAndReservationTest {

    private final User user = TestFixtures.customer();
    private final Vehicle car = TestFixtures.nexon(user);
    private final ChargingUnit unit = TestFixtures.unit(TestFixtures.station24h(), "VX-1", ConnectorType.CCS2, "60",
            PricingPlan.STANDARD);
    private final LocalDateTime start = LocalDateTime.of(2026, 11, 10, 10, 0);

    @Test
    void reservationLifecycle() {
        Reservation r = new Reservation(user, car, unit, start, start.plusHours(1));
        assertThat(r.isWithinStartWindow(start.minusMinutes(10), 15)).isTrue();
        assertThat(r.isWithinStartWindow(start.minusMinutes(20), 15)).isFalse();
        assertThat(r.overlaps(start.plusMinutes(30), start.plusMinutes(90))).isTrue();
        assertThat(r.overlaps(start.plusHours(1), start.plusHours(2))).isFalse(); // touching is not overlapping
        r.activate();
        assertThatThrownBy(() -> r.cancel("too late", start)).isInstanceOf(InvalidBookingException.class);
        r.complete();
        assertThat(r.getStatus()).isEqualTo(ReservationStatus.COMPLETED);
    }

    @Test
    void endMustBeAfterStart() {
        assertThatThrownBy(() -> new Reservation(user, car, unit, start, start)).isInstanceOf(InvalidBookingException.class);
    }

    @Test
    void sessionCanOnlyCompleteOnceAndInvoiceAddsGst() {
        Reservation r = new Reservation(user, car, unit, start, start.plusHours(1));
        r.activate();
        ChargingSession session = new ChargingSession(r, start, 30, 80, new BigDecimal("50"));
        PriceBreakdown price = new StandardPricingStrategy().calculatePrice(new BigDecimal("20.25"), unit.getPricePerKwh(), start);

        assertThatThrownBy(() -> Invoice.issue(session, price, new BigDecimal("0.18"), start))
                .isInstanceOf(BusinessRuleViolationException.class);

        session.complete(start.plusMinutes(40), 80, new BigDecimal("20.25"), 40, price.subtotal());
        assertThatThrownBy(() -> session.complete(start.plusMinutes(41), 80, BigDecimal.ONE, 41, BigDecimal.ONE))
                .isInstanceOf(InvalidSessionException.class);

        Invoice invoice = Invoice.issue(session, price, new BigDecimal("0.18"), start.plusMinutes(40));
        // 20.25 kWh × ₹20 = ₹405.00; GST 18% = ₹72.90; total ₹477.90
        assertThat(invoice.getSubtotal()).isEqualByComparingTo("405.00");
        assertThat(invoice.getTaxAmount()).isEqualByComparingTo("72.90");
        assertThat(invoice.getTotalAmount()).isEqualByComparingTo("477.90");
        invoice.markPaid(start.plusHours(1));
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThatThrownBy(() -> invoice.markPaid(start.plusHours(2))).isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void sessionTargetMustExceedCurrentBattery() {
        Reservation r = new Reservation(user, car, unit, start, start.plusHours(1));
        assertThatThrownBy(() -> new ChargingSession(r, start, 80, 70, new BigDecimal("50")))
                .isInstanceOf(InvalidSessionException.class);
    }
}
