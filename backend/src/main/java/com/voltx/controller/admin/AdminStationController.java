package com.voltx.controller.admin;

import com.voltx.dto.request.ChargerRequest;
import com.voltx.dto.request.ChargerStatusRequest;
import com.voltx.dto.request.ChargerUpdateRequest;
import com.voltx.dto.request.StationRequest;
import com.voltx.dto.response.ChargerResponse;
import com.voltx.dto.response.StationResponse;
import com.voltx.security.VoltXUserDetails;
import com.voltx.service.ChargerService;
import com.voltx.service.StationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Infrastructure management. Protected twice: URL rule in SecurityConfig and {@code @PreAuthorize}. */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin – stations & chargers")
public class AdminStationController {

    private final StationService stationService;
    private final ChargerService chargerService;

    public AdminStationController(StationService stationService, ChargerService chargerService) {
        this.stationService = stationService;
        this.chargerService = chargerService;
    }

    @GetMapping("/stations")
    @Operation(summary = "All stations including disabled ones")
    public List<StationResponse> stations() {
        return stationService.listAll();
    }

    @GetMapping("/stations/{id}")
    public StationResponse station(@PathVariable Long id) {
        return stationService.get(id, null, null);
    }

    @PostMapping("/stations")
    @ResponseStatus(HttpStatus.CREATED)
    public StationResponse createStation(@AuthenticationPrincipal VoltXUserDetails me,
                                         @Valid @RequestBody StationRequest request) {
        return stationService.create(me.getUsername(), request);
    }

    @PutMapping("/stations/{id}")
    public StationResponse updateStation(@AuthenticationPrincipal VoltXUserDetails me, @PathVariable Long id,
                                         @Valid @RequestBody StationRequest request) {
        return stationService.update(me.getUsername(), id, request);
    }

    @PatchMapping("/stations/{id}/enable")
    public StationResponse enable(@AuthenticationPrincipal VoltXUserDetails me, @PathVariable Long id) {
        return stationService.setEnabled(me.getUsername(), id, true);
    }

    @PatchMapping("/stations/{id}/disable")
    @Operation(summary = "Disable a station; upcoming reservations there are cancelled")
    public StationResponse disable(@AuthenticationPrincipal VoltXUserDetails me, @PathVariable Long id) {
        return stationService.setEnabled(me.getUsername(), id, false);
    }

    @GetMapping("/chargers")
    public List<ChargerResponse> chargers(@RequestParam(required = false) Long stationId) {
        return chargerService.listAll(stationId);
    }

    @PostMapping("/chargers")
    @ResponseStatus(HttpStatus.CREATED)
    public ChargerResponse createCharger(@AuthenticationPrincipal VoltXUserDetails me,
                                         @Valid @RequestBody ChargerRequest request) {
        return chargerService.create(me.getUsername(), request);
    }

    @PutMapping("/chargers/{id}")
    @Operation(summary = "Update connector, power and pricing")
    public ChargerResponse updateCharger(@AuthenticationPrincipal VoltXUserDetails me, @PathVariable Long id,
                                         @Valid @RequestBody ChargerUpdateRequest request) {
        return chargerService.update(me.getUsername(), id, request);
    }

    @PatchMapping("/chargers/{id}/status")
    @Operation(summary = "Mark out of service / maintenance, or restore (validated by the state machine)")
    public ChargerResponse changeStatus(@AuthenticationPrincipal VoltXUserDetails me, @PathVariable Long id,
                                        @Valid @RequestBody ChargerStatusRequest request) {
        return chargerService.changeStatus(me.getUsername(), id, request);
    }
}
