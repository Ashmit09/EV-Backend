package com.voltx.controller;

import com.voltx.dto.request.CancelReservationRequest;
import com.voltx.dto.request.ReservationRequest;
import com.voltx.dto.response.CostEstimateResponse;
import com.voltx.dto.response.ReservationResponse;
import com.voltx.security.VoltXUserDetails;
import com.voltx.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@Tag(name = "Reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Book a charging slot (409 on overlap, 422 if the vehicle is incompatible)")
    public ReservationResponse create(@AuthenticationPrincipal VoltXUserDetails me,
                                      @Valid @RequestBody ReservationRequest request) {
        return reservationService.create(me.getId(), request);
    }

    @PostMapping("/estimate")
    @Operation(summary = "Simulated energy and cost estimate for a prospective booking")
    public CostEstimateResponse estimate(@AuthenticationPrincipal VoltXUserDetails me,
                                         @Valid @RequestBody ReservationRequest request) {
        return reservationService.estimate(me.getId(), request);
    }

    @GetMapping("/my")
    public List<ReservationResponse> mine(@AuthenticationPrincipal VoltXUserDetails me) {
        return reservationService.myReservations(me.getId());
    }

    @GetMapping("/{id}")
    public ReservationResponse get(@AuthenticationPrincipal VoltXUserDetails me, @PathVariable Long id) {
        return reservationService.get(me.getId(), id);
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancel a confirmed reservation and release the charger")
    public ReservationResponse cancel(@AuthenticationPrincipal VoltXUserDetails me, @PathVariable Long id,
                                      @Valid @RequestBody(required = false) CancelReservationRequest request) {
        return reservationService.cancel(me.getId(), id, request == null ? null : request.reason());
    }
}
