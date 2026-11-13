package com.voltx.service.impl;

import com.voltx.dto.request.ChargerRequest;
import com.voltx.dto.request.ChargerStatusRequest;
import com.voltx.dto.request.ChargerUpdateRequest;
import com.voltx.dto.response.BookedSlotResponse;
import com.voltx.dto.response.ChargerResponse;
import com.voltx.entity.ChargingStation;
import com.voltx.entity.ChargingUnit;
import com.voltx.enums.AuditAction;
import com.voltx.enums.ChargerStatus;
import com.voltx.enums.ReservationStatus;
import com.voltx.exception.ChargerNotFoundException;
import com.voltx.exception.DuplicateResourceException;
import com.voltx.exception.InvalidChargingStateException;
import com.voltx.exception.StationNotFoundException;
import com.voltx.mapper.StationMapper;
import com.voltx.repository.ChargingUnitRepository;
import com.voltx.repository.ReservationRepository;
import com.voltx.repository.StationRepository;
import com.voltx.service.AuditService;
import com.voltx.service.ChargerService;
import com.voltx.service.ReservationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Service
public class ChargerServiceImpl implements ChargerService {

    private final ChargingUnitRepository unitRepository;
    private final StationRepository stationRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationService reservationService;
    private final AuditService auditService;

    public ChargerServiceImpl(ChargingUnitRepository unitRepository, StationRepository stationRepository,
                              ReservationRepository reservationRepository, ReservationService reservationService,
                              AuditService auditService) {
        this.unitRepository = unitRepository;
        this.stationRepository = stationRepository;
        this.reservationRepository = reservationRepository;
        this.reservationService = reservationService;
        this.auditService = auditService;
    }

    @Override
    @Transactional(readOnly = true)
    public ChargerResponse get(Long chargerId) {
        return StationMapper.toChargerResponse(findUnit(chargerId), null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookedSlotResponse> bookedSlots(Long chargerId, LocalDate date) {
        findUnit(chargerId);
        return reservationRepository.findOnUnitBetween(chargerId, date.atStartOfDay(),
                        date.plusDays(1).atStartOfDay(), ReservationStatus.SLOT_HOLDING).stream()
                .map(r -> new BookedSlotResponse(r.getStartTime(), r.getEndTime()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChargerResponse> listAll(Long stationId) {
        List<ChargingUnit> units = stationId == null
                ? unitRepository.findAllWithStation()
                : unitRepository.findByStationIdOrderByChargerCode(stationId);
        return units.stream().map(unit -> StationMapper.toChargerResponse(unit, null)).toList();
    }

    @Override
    @Transactional
    public ChargerResponse create(String adminEmail, ChargerRequest request) {
        ChargingStation station = stationRepository.findById(request.stationId())
                .orElseThrow(() -> new StationNotFoundException(request.stationId()));
        String code = request.chargerCode().trim().toUpperCase(Locale.ROOT);
        if (unitRepository.existsByChargerCodeIgnoreCase(code)) {
            throw new DuplicateResourceException("Charger code " + code + " is already in use");
        }
        ChargingUnit unit = unitRepository.save(new ChargingUnit(station, code, request.connectorType(),
                request.maxPowerKw(), request.pricePerKwh(), request.pricingPlan()));
        auditService.record(adminEmail, AuditAction.CHARGER_CREATED, "ChargingUnit", unit.getId(),
                code + " at " + station.getName());
        return StationMapper.toChargerResponse(unit, null);
    }

    @Override
    @Transactional
    public ChargerResponse update(String adminEmail, Long chargerId, ChargerUpdateRequest request) {
        ChargingUnit unit = unitRepository.findByIdForUpdate(chargerId)
                .orElseThrow(() -> new ChargerNotFoundException(chargerId));
        boolean hardwareChanged = unit.getConnectorType() != request.connectorType()
                || unit.getMaxPowerKw().compareTo(request.maxPowerKw()) != 0;
        boolean inUse = unit.getStatus() == ChargerStatus.RESERVED || unit.getStatus() == ChargerStatus.CHARGING;
        if (hardwareChanged && inUse) {
            throw new InvalidChargingStateException(
                    "Connector and power cannot be changed while " + unit.getChargerCode() + " is " + unit.getStatus());
        }
        unit.updateSpecifications(request.connectorType(), request.maxPowerKw());
        // Running sessions keep the price they started with (snapshotted in ChargingSession).
        unit.updatePricing(request.pricePerKwh(), request.pricingPlan());
        auditService.record(adminEmail, AuditAction.CHARGER_UPDATED, "ChargingUnit", chargerId, String.format(
                "%s: %s %s kW, ₹%s/kWh, %s", unit.getChargerCode(), unit.getConnectorType().getLabel(),
                unit.getMaxPowerKw().toPlainString(), unit.getPricePerKwh().toPlainString(), unit.getPricingPlan()));
        return StationMapper.toChargerResponse(unit, null);
    }

    @Override
    @Transactional
    public ChargerResponse changeStatus(String adminEmail, Long chargerId, ChargerStatusRequest request) {
        ChargingUnit unit = unitRepository.findByIdForUpdate(chargerId)
                .orElseThrow(() -> new ChargerNotFoundException(chargerId));
        ChargerStatus before = unit.getStatus();
        int cancelled = 0;
        switch (request.action()) {
            case MARK_OUT_OF_SERVICE -> {
                unit.markOutOfService();
                cancelled = reservationService.cancelUpcomingForCharger(chargerId,
                        "Charger taken out of service", adminEmail);
            }
            case MARK_MAINTENANCE -> {
                unit.markUnderMaintenance();
                cancelled = reservationService.cancelUpcomingForCharger(chargerId,
                        "Charger under scheduled maintenance", adminEmail);
            }
            case RESTORE -> unit.restoreService();
        }
        auditService.record(adminEmail, AuditAction.CHARGER_STATUS_CHANGED, "ChargingUnit", chargerId,
                unit.getChargerCode() + ": " + before + " → " + unit.getStatus()
                        + (cancelled > 0 ? " (" + cancelled + " reservations cancelled)" : ""));
        return StationMapper.toChargerResponse(unit, null);
    }

    private ChargingUnit findUnit(Long chargerId) {
        return unitRepository.findById(chargerId).orElseThrow(() -> new ChargerNotFoundException(chargerId));
    }
}
