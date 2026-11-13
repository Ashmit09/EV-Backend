package com.voltx.controller.admin;

import com.voltx.dto.request.CancelReservationRequest;
import com.voltx.dto.response.AdminOverviewResponse;
import com.voltx.dto.response.AuditLogResponse;
import com.voltx.dto.response.EnergyAnalyticsResponse;
import com.voltx.dto.response.InvoiceResponse;
import com.voltx.dto.response.PageResponse;
import com.voltx.dto.response.PaymentResponse;
import com.voltx.dto.response.ReservationResponse;
import com.voltx.dto.response.RevenueAnalyticsResponse;
import com.voltx.dto.response.SessionResponse;
import com.voltx.dto.response.UserResponse;
import com.voltx.enums.AuditAction;
import com.voltx.enums.ReservationStatus;
import com.voltx.enums.Role;
import com.voltx.security.VoltXUserDetails;
import com.voltx.service.AnalyticsService;
import com.voltx.service.AuditService;
import com.voltx.service.BillingService;
import com.voltx.service.ChargingSessionService;
import com.voltx.service.ReservationService;
import com.voltx.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin – operations & analytics")
public class AdminOperationsController {

    private final UserService userService;
    private final ReservationService reservationService;
    private final ChargingSessionService sessionService;
    private final BillingService billingService;
    private final AnalyticsService analyticsService;
    private final AuditService auditService;

    public AdminOperationsController(UserService userService, ReservationService reservationService,
                                     ChargingSessionService sessionService, BillingService billingService,
                                     AnalyticsService analyticsService, AuditService auditService) {
        this.userService = userService;
        this.reservationService = reservationService;
        this.sessionService = sessionService;
        this.billingService = billingService;
        this.analyticsService = analyticsService;
        this.auditService = auditService;
    }

    @GetMapping("/users")
    public List<UserResponse> users(@RequestParam(required = false) Role role) {
        return userService.listUsers(role);
    }

    @PatchMapping("/users/{id}/enable")
    public UserResponse enableUser(@AuthenticationPrincipal VoltXUserDetails me, @PathVariable Long id) {
        return userService.setEnabled(me.getUsername(), id, true);
    }

    @PatchMapping("/users/{id}/disable")
    public UserResponse disableUser(@AuthenticationPrincipal VoltXUserDetails me, @PathVariable Long id) {
        return userService.setEnabled(me.getUsername(), id, false);
    }

    @GetMapping("/reservations")
    public List<ReservationResponse> reservations(@RequestParam(required = false) ReservationStatus status) {
        return reservationService.listAll(status);
    }

    @PatchMapping("/reservations/{id}/cancel")
    public ReservationResponse cancelReservation(@AuthenticationPrincipal VoltXUserDetails me, @PathVariable Long id,
                                                 @Valid @RequestBody(required = false) CancelReservationRequest request) {
        return reservationService.cancelAsAdmin(me.getUsername(), id, request == null ? null : request.reason());
    }

    @GetMapping("/sessions/active")
    @Operation(summary = "All running sessions with live progress")
    public List<SessionResponse> activeSessions() {
        return sessionService.listActive();
    }

    @PostMapping("/sessions/{id}/stop")
    public SessionResponse stopSession(@AuthenticationPrincipal VoltXUserDetails me, @PathVariable Long id) {
        return sessionService.stopAsAdmin(me.getUsername(), id);
    }

    @GetMapping("/invoices")
    public List<InvoiceResponse> invoices() {
        return billingService.listAll();
    }

    @GetMapping("/payments")
    public List<PaymentResponse> payments() {
        return billingService.listPayments();
    }

    @GetMapping("/analytics/overview")
    public AdminOverviewResponse overview() {
        return analyticsService.overview();
    }

    @GetMapping("/analytics/revenue")
    public RevenueAnalyticsResponse revenue(@RequestParam(defaultValue = "6") int months) {
        return analyticsService.revenue(months);
    }

    @GetMapping("/analytics/energy")
    public EnergyAnalyticsResponse energy(@RequestParam(defaultValue = "6") int months) {
        return analyticsService.energy(months);
    }

    @GetMapping("/audit-logs")
    public PageResponse<AuditLogResponse> auditLogs(@RequestParam(required = false) AuditAction action,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "25") int size) {
        return auditService.list(action, page, size);
    }
}
