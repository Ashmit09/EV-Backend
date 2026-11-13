package com.voltx.service.impl;

import com.voltx.dto.request.StationRequest;
import com.voltx.dto.response.ChargerResponse;
import com.voltx.dto.response.StationResponse;
import com.voltx.entity.ChargingStation;
import com.voltx.entity.Vehicle;
import com.voltx.enums.AuditAction;
import com.voltx.enums.ConnectorType;
import com.voltx.enums.SessionStatus;
import com.voltx.enums.StationStatus;
import com.voltx.exception.BusinessRuleViolationException;
import com.voltx.exception.DuplicateResourceException;
import com.voltx.exception.StationNotFoundException;
import com.voltx.exception.VehicleNotFoundException;
import com.voltx.mapper.StationMapper;
import com.voltx.repository.ChargingSessionRepository;
import com.voltx.repository.StationRepository;
import com.voltx.repository.VehicleRepository;
import com.voltx.service.AuditService;
import com.voltx.service.ReservationService;
import com.voltx.service.StationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.Predicate;

@Service
public class StationServiceImpl implements StationService {

    private final StationRepository stationRepository;
    private final VehicleRepository vehicleRepository;
    private final ChargingSessionRepository sessionRepository;
    private final ReservationService reservationService;
    private final AuditService auditService;

    public StationServiceImpl(StationRepository stationRepository, VehicleRepository vehicleRepository,
                              ChargingSessionRepository sessionRepository, ReservationService reservationService,
                              AuditService auditService) {
        this.stationRepository = stationRepository;
        this.vehicleRepository = vehicleRepository;
        this.sessionRepository = sessionRepository;
        this.reservationService = reservationService;
        this.auditService = auditService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StationResponse> search(String city, String keyword, ConnectorType connector, boolean availableOnly) {
        // Filters are composed as Predicate lambdas; a filter that was not requested is "always true".
        Predicate<ChargingStation> hasConnector = connector == null
                ? station -> true
                : station -> station.getChargingUnits().stream().anyMatch(u -> u.getConnectorType() == connector);
        Predicate<ChargingStation> hasAvailability = availableOnly
                ? station -> station.countAvailableUnits() > 0
                : station -> true;

        return stationRepository.search(blankToNull(city), blankToNull(keyword), StationStatus.ACTIVE).stream()
                .filter(hasConnector.and(hasAvailability))
                .map(station -> StationMapper.toResponse(station, false, null))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> cities() {
        return stationRepository.findDistinctCities(StationStatus.ACTIVE);
    }

    @Override
    @Transactional(readOnly = true)
    public StationResponse get(Long stationId, Long vehicleId, Long userId) {
        return StationMapper.toResponse(findStation(stationId), true, resolveVehicle(vehicleId, userId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChargerResponse> chargers(Long stationId, Long vehicleId, Long userId) {
        Vehicle vehicle = resolveVehicle(vehicleId, userId);
        return findStation(stationId).getChargingUnits().stream()
                .map(unit -> StationMapper.toChargerResponse(unit, vehicle))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StationResponse> listAll() {
        return stationRepository.search(null, null, null).stream()
                .map(station -> StationMapper.toResponse(station, false, null))
                .toList();
    }

    @Override
    @Transactional
    public StationResponse create(String adminEmail, StationRequest request) {
        if (stationRepository.existsByNameIgnoreCaseAndCityIgnoreCase(request.name().trim(), request.city().trim())) {
            throw new DuplicateResourceException("A station named '" + request.name() + "' already exists in " + request.city());
        }
        ChargingStation station = stationRepository.save(new ChargingStation(request.name(), request.address(),
                request.city(), request.latitude(), request.longitude(), request.openingTime(),
                request.closingTime(), request.open24Hours()));
        auditService.record(adminEmail, AuditAction.STATION_CREATED, "ChargingStation", station.getId(),
                station.getName() + ", " + station.getCity());
        return StationMapper.toResponse(station, true, null);
    }

    @Override
    @Transactional
    public StationResponse update(String adminEmail, Long stationId, StationRequest request) {
        ChargingStation station = findStation(stationId);
        if (stationRepository.existsByNameIgnoreCaseAndCityIgnoreCaseAndIdNot(
                request.name().trim(), request.city().trim(), stationId)) {
            throw new DuplicateResourceException("A station named '" + request.name() + "' already exists in " + request.city());
        }
        station.updateDetails(request.name(), request.address(), request.city(), request.latitude(),
                request.longitude(), request.openingTime(), request.closingTime(), request.open24Hours());
        auditService.record(adminEmail, AuditAction.STATION_UPDATED, "ChargingStation", stationId, station.getName());
        return StationMapper.toResponse(station, true, null);
    }

    @Override
    @Transactional
    public StationResponse setEnabled(String adminEmail, Long stationId, boolean enabled) {
        ChargingStation station = findStation(stationId);
        if (enabled) {
            station.enable();
            auditService.record(adminEmail, AuditAction.STATION_ENABLED, "ChargingStation", stationId, station.getName());
        } else {
            if (sessionRepository.existsByChargingUnitStationIdAndStatus(stationId, SessionStatus.IN_PROGRESS)) {
                throw new BusinessRuleViolationException(
                        "Stop the active charging sessions at this station before disabling it");
            }
            int cancelled = reservationService.cancelUpcomingForStation(stationId,
                    "Station temporarily closed by VoltX", adminEmail);
            station.disable();
            auditService.record(adminEmail, AuditAction.STATION_DISABLED, "ChargingStation", stationId,
                    station.getName() + " (" + cancelled + " upcoming reservations cancelled)");
        }
        return StationMapper.toResponse(station, true, null);
    }

    private ChargingStation findStation(Long stationId) {
        return stationRepository.findById(stationId).orElseThrow(() -> new StationNotFoundException(stationId));
    }

    private Vehicle resolveVehicle(Long vehicleId, Long userId) {
        if (vehicleId == null || userId == null) {
            return null;
        }
        return vehicleRepository.findByIdAndOwnerId(vehicleId, userId)
                .orElseThrow(() -> new VehicleNotFoundException(vehicleId));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
