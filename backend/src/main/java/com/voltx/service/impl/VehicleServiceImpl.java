package com.voltx.service.impl;

import com.voltx.dto.request.VehicleRequest;
import com.voltx.dto.response.VehicleResponse;
import com.voltx.entity.User;
import com.voltx.entity.Vehicle;
import com.voltx.enums.AuditAction;
import com.voltx.enums.SessionStatus;
import com.voltx.exception.BusinessRuleViolationException;
import com.voltx.exception.DuplicateResourceException;
import com.voltx.exception.UserNotFoundException;
import com.voltx.exception.VehicleNotFoundException;
import com.voltx.mapper.VehicleMapper;
import com.voltx.repository.ChargingSessionRepository;
import com.voltx.repository.ReservationRepository;
import com.voltx.repository.UserRepository;
import com.voltx.repository.VehicleRepository;
import com.voltx.service.AuditService;
import com.voltx.service.VehicleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class VehicleServiceImpl implements VehicleService {

    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;
    private final ChargingSessionRepository sessionRepository;
    private final AuditService auditService;

    public VehicleServiceImpl(VehicleRepository vehicleRepository, UserRepository userRepository,
                              ReservationRepository reservationRepository, ChargingSessionRepository sessionRepository,
                              AuditService auditService) {
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
        this.reservationRepository = reservationRepository;
        this.sessionRepository = sessionRepository;
        this.auditService = auditService;
    }

    @Override
    @Transactional
    public VehicleResponse create(Long userId, VehicleRequest request) {
        User owner = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        String registration = normalize(request.registrationNumber());
        if (vehicleRepository.existsByRegistrationNumberIgnoreCase(registration)) {
            throw new DuplicateResourceException("A vehicle with registration " + registration + " is already registered");
        }
        Vehicle vehicle = vehicleRepository.save(VehicleFactory.create(owner, request));
        auditService.record(owner.getEmail(), AuditAction.VEHICLE_ADDED, "Vehicle", vehicle.getId(),
                vehicle.getDisplayName() + " (" + vehicle.getRegistrationNumber() + ")");
        return VehicleMapper.toResponse(vehicle);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleResponse> list(Long userId) {
        return vehicleRepository.findByOwnerIdOrderByCreatedAtDesc(userId).stream()
                .map(VehicleMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleResponse get(Long userId, Long vehicleId) {
        return VehicleMapper.toResponse(findOwned(userId, vehicleId));
    }

    @Override
    @Transactional
    public VehicleResponse update(Long userId, Long vehicleId, VehicleRequest request) {
        Vehicle vehicle = findOwned(userId, vehicleId);
        if (request.vehicleType() != vehicle.getVehicleType()) {
            throw new BusinessRuleViolationException("Vehicle type cannot be changed; remove the vehicle and add it again");
        }
        if (!normalize(request.registrationNumber()).equals(vehicle.getRegistrationNumber())) {
            throw new BusinessRuleViolationException("Registration number cannot be changed after registration");
        }
        if (sessionRepository.existsByVehicleIdAndStatus(vehicleId, SessionStatus.IN_PROGRESS)) {
            throw new BusinessRuleViolationException("Vehicle details cannot be edited while it is charging");
        }
        vehicle.updateDetails(request.manufacturer(), request.model(), request.batteryCapacityKwh(),
                request.maxChargingPowerKw(), request.supportedConnectors());
        vehicle.updateBatteryLevel(request.currentBatteryPercentage());
        auditService.record(vehicle.getOwner().getEmail(), AuditAction.VEHICLE_UPDATED, "Vehicle", vehicleId,
                vehicle.getDisplayName());
        return VehicleMapper.toResponse(vehicle);
    }

    @Override
    @Transactional
    public void delete(Long userId, Long vehicleId) {
        Vehicle vehicle = findOwned(userId, vehicleId);
        if (reservationRepository.existsByVehicleId(vehicleId) || sessionRepository.existsByVehicleId(vehicleId)) {
            throw new BusinessRuleViolationException(
                    "This vehicle has bookings or charging history and cannot be removed");
        }
        vehicleRepository.delete(vehicle);
        auditService.record(vehicle.getOwner().getEmail(), AuditAction.VEHICLE_REMOVED, "Vehicle", vehicleId,
                vehicle.getDisplayName());
    }

    private Vehicle findOwned(Long userId, Long vehicleId) {
        return vehicleRepository.findByIdAndOwnerId(vehicleId, userId)
                .orElseThrow(() -> new VehicleNotFoundException(vehicleId));
    }

    private static String normalize(String registration) {
        return registration.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
    }
}
