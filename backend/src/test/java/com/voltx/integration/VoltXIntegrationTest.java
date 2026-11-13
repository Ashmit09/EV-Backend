package com.voltx.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voltx.entity.User;
import com.voltx.enums.Role;
import com.voltx.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests through the real HTTP layer, security filter chain, services, JPA and
 * Flyway migrations (on an in-memory H2 database in MySQL mode).
 */
@SpringBootTest
@AutoConfigureMockMvc
class VoltXIntegrationTest {

    private static final String ADMIN_EMAIL = "it-admin@voltx.in";
    private static final String PASSWORD = "Password123";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper json;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void ensureAdmin() {
        if (!userRepository.existsByEmailIgnoreCase(ADMIN_EMAIL)) {
            userRepository.save(new User("IT Admin", ADMIN_EMAIL, passwordEncoder.encode(PASSWORD), null, Role.ADMIN));
        }
    }

    // ------------------------------------------------------------------ authentication

    @Test
    @DisplayName("Registration succeeds, duplicate e-mail is rejected, login validates credentials")
    void registrationAndLogin() throws Exception {
        String email = uniqueEmail();
        call(post("/api/auth/register"), null, Map.of("fullName", "New User", "email", email, "password", PASSWORD))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist());

        call(post("/api/auth/register"), null, Map.of("fullName", "Again", "email", email.toUpperCase(), "password", PASSWORD))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_RESOURCE"));

        call(post("/api/auth/login"), null, Map.of("email", email, "password", "WrongPass1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));

        call(post("/api/auth/register"), null, Map.of("fullName", "X", "email", "not-an-email", "password", "short"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    @DisplayName("Protected and admin-only endpoints reject anonymous users and customers")
    void accessControl() throws Exception {
        String customer = registerCustomer();
        mvc.perform(get("/api/vehicles")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/analytics/overview").header("Authorization", bearer(customer)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/stations")).andExpect(status().isOk());
        String admin = login(ADMIN_EMAIL);
        mvc.perform(get("/api/admin/analytics/overview").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalStations").value(org.hamcrest.Matchers.greaterThanOrEqualTo(10)));
        // admins do not use customer endpoints
        mvc.perform(get("/api/vehicles").header("Authorization", bearer(admin))).andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------ main customer workflow

    @Test
    @DisplayName("Book → overlap rejected → start → stop → invoice → pay, with ownership enforced")
    void fullChargingWorkflow() throws Exception {
        String customer = registerCustomer();
        long vehicleId = addCar(customer);
        long chargerId = chargerIdByCode("VX-HYD-01-B");
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Kolkata")).truncatedTo(ChronoUnit.MINUTES);

        // compatibility annotation on station chargers
        JsonNode chargers = read(mvc.perform(get("/api/stations/" + stationIdOf(chargerId) + "/chargers")
                .param("vehicleId", String.valueOf(vehicleId)).header("Authorization", bearer(customer))));
        assertThat(chargers.findValues("compatible")).isNotEmpty();

        Map<String, Object> booking = Map.of("vehicleId", vehicleId, "chargingUnitId", chargerId,
                "startTime", now.toString(), "durationMinutes", 60);
        JsonNode reservation = read(call(post("/api/reservations"), customer, booking).andExpect(status().isCreated()));
        long reservationId = reservation.get("id").asLong();
        assertThat(reservation.get("canStart").asBoolean()).isTrue();
        mvc.perform(get("/api/chargers/" + chargerId)).andExpect(jsonPath("$.status").value("RESERVED"));

        // a second customer cannot book an overlapping slot
        String other = registerCustomer();
        long otherVehicle = addCar(other);
        call(post("/api/reservations"), other, Map.of("vehicleId", otherVehicle, "chargingUnitId", chargerId,
                "startTime", now.plusMinutes(30).toString(), "durationMinutes", 60))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BOOKING_CONFLICT"));

        // start charging
        JsonNode session = read(call(post("/api/sessions/start"), customer, Map.of("reservationId", reservationId))
                .andExpect(status().isCreated()));
        long sessionId = session.get("id").asLong();
        assertThat(session.get("status").asText()).isEqualTo("IN_PROGRESS");
        mvc.perform(get("/api/chargers/" + chargerId)).andExpect(jsonPath("$.status").value("CHARGING"));

        // starting twice is an invalid state transition
        call(post("/api/sessions/start"), customer, Map.of("reservationId", reservationId))
                .andExpect(status().isConflict());

        // other customers cannot see or stop this session
        mvc.perform(get("/api/sessions/" + sessionId).header("Authorization", bearer(other)))
                .andExpect(status().isForbidden());

        Thread.sleep(50); // with the test time multiplier this is over an hour of simulated charging
        JsonNode stopped = read(call(post("/api/sessions/" + sessionId + "/stop"), customer, null)
                .andExpect(status().isOk()));
        assertThat(stopped.get("status").asText()).isEqualTo("COMPLETED");
        double initial = stopped.get("initialBatteryPercentage").asDouble();
        double finalPct = stopped.get("currentBatteryPercentage").asDouble();
        assertThat(finalPct).isEqualTo(80.0); // car on DC stops at 80%
        // energy = 40.5 kWh × (80 − 30) / 100 = 20.25 kWh
        assertThat(stopped.get("energyKwh").decimalValue()).isEqualByComparingTo("20.250");
        assertThat(initial).isEqualTo(30.0);
        mvc.perform(get("/api/chargers/" + chargerId)).andExpect(jsonPath("$.status").value("AVAILABLE"));
        mvc.perform(get("/api/vehicles/" + vehicleId).header("Authorization", bearer(customer)))
                .andExpect(jsonPath("$.currentBatteryPercentage").value(80));

        // invoice: 20.25 kWh × ₹18.50 = ₹374.63 (+18% GST = ₹67.43) = ₹442.06
        long invoiceId = stopped.get("invoiceId").asLong();
        mvc.perform(get("/api/invoices/" + invoiceId).header("Authorization", bearer(customer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotal").value(374.63))
                .andExpect(jsonPath("$.taxAmount").value(67.43))
                .andExpect(jsonPath("$.totalAmount").value(442.06))
                .andExpect(jsonPath("$.status").value("UNPAID"));
        mvc.perform(get("/api/invoices/" + invoiceId).header("Authorization", bearer(other)))
                .andExpect(status().isForbidden());

        call(post("/api/invoices/" + invoiceId + "/pay"), customer, Map.of("method", "UPI"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.payments[0].status").value("SUCCESS"));
        call(post("/api/invoices/" + invoiceId + "/pay"), customer, Map.of("method", "UPI"))
                .andExpect(status().isUnprocessableEntity());

        // history and analytics come from the database
        mvc.perform(get("/api/sessions").header("Authorization", bearer(customer)))
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/analytics/customer").header("Authorization", bearer(customer)))
                .andExpect(jsonPath("$.totalSessions").value(1))
                .andExpect(jsonPath("$.totalEnergyKwh").value(20.25));
    }

    @Test
    @DisplayName("Cancelling a reservation releases the charger")
    void cancellationReleasesCharger() throws Exception {
        String customer = registerCustomer();
        long vehicleId = addCar(customer);
        long chargerId = chargerIdByCode("VX-CHN-01-A");
        // Chennai station closes at 22:00; book tomorrow morning so the test passes at any time of day
        LocalDateTime tomorrow = LocalDateTime.now(ZoneId.of("Asia/Kolkata")).plusDays(1).withHour(9).truncatedTo(ChronoUnit.HOURS);
        long id = read(call(post("/api/reservations"), customer, Map.of("vehicleId", vehicleId,
                "chargingUnitId", chargerId, "startTime", tomorrow.toString(), "durationMinutes", 45))
                .andExpect(status().isCreated())).get("id").asLong();

        call(patch("/api/reservations/" + id + "/cancel"), customer, Map.of("reason", "Plans changed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        call(patch("/api/reservations/" + id + "/cancel"), customer, null).andExpect(status().isBadRequest());

        // the same slot can now be booked again
        call(post("/api/reservations"), customer, Map.of("vehicleId", vehicleId, "chargingUnitId", chargerId,
                "startTime", tomorrow.toString(), "durationMinutes", 45)).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Incompatible connector bookings are rejected with 422")
    void incompatibleConnector() throws Exception {
        String customer = registerCustomer();
        long vehicleId = addCar(customer); // CCS2 + Type 2
        long chademo = chargerIdByCode("VX-CHN-01-B");
        call(post("/api/reservations"), customer, Map.of("vehicleId", vehicleId, "chargingUnitId", chademo,
                "startTime", LocalDateTime.now(ZoneId.of("Asia/Kolkata")).plusDays(1).withHour(10).truncatedTo(ChronoUnit.HOURS).toString(),
                "durationMinutes", 30))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INCOMPATIBLE_CONNECTOR"));
    }

    // ------------------------------------------------------------------ admin

    @Test
    @DisplayName("Admin registers a station and charger, and invalid state transitions are refused")
    void adminManagesInfrastructure() throws Exception {
        String admin = login(ADMIN_EMAIL);
        String name = "IT Station " + UUID.randomUUID().toString().substring(0, 6);
        long stationId = read(call(post("/api/admin/stations"), admin, Map.of("name", name, "address", "1 Test Rd",
                "city", "Jaipur", "latitude", 26.9, "longitude", 75.8, "openingTime", "06:00", "closingTime", "22:00",
                "open24Hours", false)).andExpect(status().isCreated())).get("id").asLong();

        String code = "VX-IT-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        long chargerId = read(call(post("/api/admin/chargers"), admin, Map.of("stationId", stationId, "chargerCode", code,
                "connectorType", "CCS2", "maxPowerKw", 60, "pricePerKwh", 19.5, "pricingPlan", "PEAK_HOUR"))
                .andExpect(status().isCreated())).get("id").asLong();

        call(post("/api/admin/chargers"), admin, Map.of("stationId", stationId, "chargerCode", code,
                "connectorType", "CCS2", "maxPowerKw", 60, "pricePerKwh", 19.5, "pricingPlan", "STANDARD"))
                .andExpect(status().isConflict());

        call(patch("/api/admin/chargers/" + chargerId + "/status"), admin, Map.of("action", "RESTORE"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_CHARGER_STATE"));
        call(patch("/api/admin/chargers/" + chargerId + "/status"), admin, Map.of("action", "MARK_MAINTENANCE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("MAINTENANCE"));

        call(patch("/api/admin/stations/" + stationId + "/disable"), admin, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISABLED"));

        mvc.perform(get("/api/admin/audit-logs").param("action", "CHARGER_STATUS_CHANGED")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").isNumber());
    }

    // ------------------------------------------------------------------ helpers

    private ResultActions call(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder,
                               String token, Object body) throws Exception {
        if (token != null) {
            builder.header("Authorization", bearer(token));
        }
        if (body != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        }
        return mvc.perform(builder);
    }

    private JsonNode read(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString());
    }

    private String registerCustomer() throws Exception {
        return read(call(post("/api/auth/register"), null,
                Map.of("fullName", "IT Customer", "email", uniqueEmail(), "password", PASSWORD)))
                .get("token").asText();
    }

    private String login(String email) throws Exception {
        return read(call(post("/api/auth/login"), null, Map.of("email", email, "password", PASSWORD))
                .andExpect(status().isOk())).get("token").asText();
    }

    private long addCar(String token) throws Exception {
        String registration = "KA01IT" + (1000 + (int) (Math.random() * 8999));
        return read(call(post("/api/vehicles"), token, Map.of("vehicleType", "CAR", "manufacturer", "Tata",
                "model", "Nexon EV", "registrationNumber", registration, "batteryCapacityKwh", 40.5,
                "maxChargingPowerKw", 50, "currentBatteryPercentage", 30,
                "supportedConnectors", new String[]{"CCS2", "TYPE_2"}))
                .andExpect(status().isCreated())).get("id").asLong();
    }

    private long chargerIdByCode(String code) throws Exception {
        JsonNode chargers = read(mvc.perform(get("/api/admin/chargers").header("Authorization", bearer(login(ADMIN_EMAIL)))));
        for (JsonNode charger : chargers) {
            if (charger.get("chargerCode").asText().equals(code)) {
                return charger.get("id").asLong();
            }
        }
        throw new IllegalStateException("Seed charger " + code + " not found");
    }

    private long stationIdOf(long chargerId) throws Exception {
        return read(mvc.perform(get("/api/chargers/" + chargerId))).get("stationId").asLong();
    }

    private static String uniqueEmail() {
        return "it-" + UUID.randomUUID().toString().substring(0, 8) + "@voltx.in";
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
