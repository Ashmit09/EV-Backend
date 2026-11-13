package com.voltx.controller;

import com.voltx.dto.response.BookedSlotResponse;
import com.voltx.dto.response.ChargerResponse;
import com.voltx.dto.response.StationResponse;
import com.voltx.enums.ConnectorType;
import com.voltx.security.VoltXUserDetails;
import com.voltx.service.ChargerService;
import com.voltx.service.StationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** Public discovery endpoints. Signed-in customers may pass {@code vehicleId} for compatibility checks. */
@RestController
@Tag(name = "Stations & chargers")
public class StationController {

    private final StationService stationService;
    private final ChargerService chargerService;

    public StationController(StationService stationService, ChargerService chargerService) {
        this.stationService = stationService;
        this.chargerService = chargerService;
    }

    @GetMapping({"/api/stations", "/api/stations/search"})
    @Operation(summary = "Search active stations by city, keyword, connector and availability")
    public List<StationResponse> search(@RequestParam(required = false) String city,
                                        @RequestParam(required = false, name = "q") String keyword,
                                        @RequestParam(required = false) ConnectorType connector,
                                        @RequestParam(defaultValue = "false") boolean availableOnly) {
        return stationService.search(city, keyword, connector, availableOnly);
    }

    @GetMapping("/api/stations/cities")
    @Operation(summary = "Cities that have at least one active station")
    public List<String> cities() {
        return stationService.cities();
    }

    @GetMapping("/api/stations/{id}")
    @Operation(summary = "Station details with chargers; pass vehicleId to check compatibility")
    public StationResponse get(@PathVariable Long id, @RequestParam(required = false) Long vehicleId,
                               @AuthenticationPrincipal VoltXUserDetails me) {
        return stationService.get(id, vehicleId, me == null ? null : me.getId());
    }

    @GetMapping("/api/stations/{id}/chargers")
    @Operation(summary = "Chargers of a station; pass vehicleId to check compatibility")
    public List<ChargerResponse> chargers(@PathVariable Long id, @RequestParam(required = false) Long vehicleId,
                                          @AuthenticationPrincipal VoltXUserDetails me) {
        return stationService.chargers(id, vehicleId, me == null ? null : me.getId());
    }

    @GetMapping("/api/chargers/{id}")
    public ChargerResponse charger(@PathVariable Long id) {
        return chargerService.get(id);
    }

    @GetMapping("/api/chargers/{id}/booked-slots")
    @Operation(summary = "Occupied time windows of a charger on a date (no customer data)")
    public List<BookedSlotResponse> bookedSlots(@PathVariable Long id,
                                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return chargerService.bookedSlots(id, date);
    }
}
