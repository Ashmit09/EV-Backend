package com.voltx.controller;

import com.voltx.dto.request.VehicleRequest;
import com.voltx.dto.response.VehicleResponse;
import com.voltx.security.VoltXUserDetails;
import com.voltx.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
@Tag(name = "Vehicles")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register an EV (car, bike or scooter)")
    public VehicleResponse create(@AuthenticationPrincipal VoltXUserDetails me, @Valid @RequestBody VehicleRequest request) {
        return vehicleService.create(me.getId(), request);
    }

    @GetMapping
    @Operation(summary = "List my vehicles")
    public List<VehicleResponse> list(@AuthenticationPrincipal VoltXUserDetails me) {
        return vehicleService.list(me.getId());
    }

    @GetMapping("/{id}")
    public VehicleResponse get(@AuthenticationPrincipal VoltXUserDetails me, @PathVariable Long id) {
        return vehicleService.get(me.getId(), id);
    }

    @PutMapping("/{id}")
    public VehicleResponse update(@AuthenticationPrincipal VoltXUserDetails me, @PathVariable Long id,
                                  @Valid @RequestBody VehicleRequest request) {
        return vehicleService.update(me.getId(), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal VoltXUserDetails me, @PathVariable Long id) {
        vehicleService.delete(me.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
