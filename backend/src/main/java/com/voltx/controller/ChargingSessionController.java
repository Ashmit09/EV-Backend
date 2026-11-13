package com.voltx.controller;

import com.voltx.dto.request.StartSessionRequest;
import com.voltx.dto.response.SessionResponse;
import com.voltx.security.VoltXUserDetails;
import com.voltx.service.ChargingSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/sessions")
@Tag(name = "Charging sessions")
public class ChargingSessionController {

    private final ChargingSessionService sessionService;

    public ChargingSessionController(ChargingSessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping("/start")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Start charging against a confirmed reservation")
    public SessionResponse start(@AuthenticationPrincipal VoltXUserDetails me,
                                 @Valid @RequestBody StartSessionRequest request) {
        return sessionService.start(me.getId(), request);
    }

    @GetMapping("/active")
    @Operation(summary = "The running session with live, server-computed progress (204 if none)")
    public ResponseEntity<SessionResponse> active(@AuthenticationPrincipal VoltXUserDetails me) {
        return sessionService.active(me.getId())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping
    @Operation(summary = "My charging history")
    public List<SessionResponse> history(@AuthenticationPrincipal VoltXUserDetails me) {
        return sessionService.history(me.getId());
    }

    @GetMapping("/{id}")
    public SessionResponse get(@AuthenticationPrincipal VoltXUserDetails me, @PathVariable Long id) {
        return sessionService.get(me.getId(), id);
    }

    @PostMapping("/{id}/stop")
    @Operation(summary = "Stop charging; returns the final summary and generates the invoice")
    public SessionResponse stop(@AuthenticationPrincipal VoltXUserDetails me, @PathVariable Long id) {
        return sessionService.stop(me.getId(), id);
    }
}
