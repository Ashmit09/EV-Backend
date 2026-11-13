package com.voltx.service.impl;

import com.voltx.config.VoltXProperties;
import com.voltx.entity.ChargingStation;
import com.voltx.exception.ChargerUnavailableException;
import com.voltx.exception.InvalidBookingException;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * The stateless booking rules, kept separate from the reservation service so they can be
 * unit-tested on their own and extended by adding one lambda to the list.
 * Rules that need the database (overlaps) live in the service, inside the locked transaction.
 */
@Component
public class BookingPolicy {

    private static final int SLOT_GRANULARITY_MINUTES = 15;
    private static final long PAST_GRACE_MINUTES = 5;
    private static final DateTimeFormatter HOURS = DateTimeFormatter.ofPattern("HH:mm");

    private final List<BookingRule> rules;

    public BookingPolicy(VoltXProperties properties) {
        VoltXProperties.Booking booking = properties.booking();
        this.rules = List.of(
                ctx -> {
                    if (ctx.durationMinutes() < booking.minDurationMinutes()
                            || ctx.durationMinutes() > booking.maxDurationMinutes()) {
                        throw new InvalidBookingException(String.format("Duration must be between %d and %d minutes",
                                booking.minDurationMinutes(), booking.maxDurationMinutes()));
                    }
                },
                ctx -> {
                    if (ctx.durationMinutes() % SLOT_GRANULARITY_MINUTES != 0) {
                        throw new InvalidBookingException("Duration must be a multiple of 15 minutes");
                    }
                },
                ctx -> {
                    if (ctx.start().isBefore(ctx.now().minusMinutes(PAST_GRACE_MINUTES))) {
                        throw new InvalidBookingException("The start time is in the past");
                    }
                },
                ctx -> {
                    if (ctx.start().isAfter(ctx.now().plusDays(booking.maxDaysInAdvance()))) {
                        throw new InvalidBookingException(
                                "Bookings can be made at most " + booking.maxDaysInAdvance() + " days in advance");
                    }
                },
                ctx -> {
                    if (!ctx.unit().getStation().isActive()) {
                        throw new ChargerUnavailableException("Station " + ctx.unit().getStation().getName()
                                + " is currently closed");
                    }
                },
                ctx -> {
                    if (!ctx.unit().acceptsBookings()) {
                        throw new ChargerUnavailableException("Charger " + ctx.unit().getChargerCode()
                                + " is " + ctx.unit().getStatus() + " and cannot be booked");
                    }
                },
                this::withinOperatingHours,
                ctx -> ctx.vehicle().checkCompatibility(ctx.unit()));
    }

    public void validate(BookingContext context) {
        rules.forEach(rule -> rule.check(context));
    }

    private void withinOperatingHours(BookingContext ctx) {
        ChargingStation station = ctx.unit().getStation();
        if (!station.isOpenDuring(ctx.start(), ctx.end())) {
            throw new InvalidBookingException(String.format("%s is open %s–%s; the booking must fit within these hours",
                    station.getName(), station.getOpeningTime().format(HOURS), station.getClosingTime().format(HOURS)));
        }
    }
}
