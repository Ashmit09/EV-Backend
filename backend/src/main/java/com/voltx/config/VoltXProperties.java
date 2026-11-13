package com.voltx.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.List;

/** Type-safe binding of the {@code voltx.*} section of application.yml. */
@ConfigurationProperties(prefix = "voltx")
public record VoltXProperties(
        String timezone,
        Jwt jwt,
        Cors cors,
        Simulation simulation,
        Booking booking,
        Billing billing,
        Seed seed) {

    public record Jwt(String secret, long expirationMinutes) {
    }

    public record Cors(List<String> allowedOrigins) {
    }

    public record Simulation(long timeMultiplier) {
    }

    public record Booking(int minDurationMinutes, int maxDurationMinutes, int maxDaysInAdvance,
                          int startWindowMinutes, int maxActiveReservations) {
    }

    public record Billing(BigDecimal gstRate) {
    }

    public record Seed(boolean enabled) {
    }
}
