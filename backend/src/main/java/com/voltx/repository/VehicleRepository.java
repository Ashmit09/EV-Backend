package com.voltx.repository;

import com.voltx.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    List<Vehicle> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    Optional<Vehicle> findByIdAndOwnerId(Long id, Long ownerId);

    boolean existsByRegistrationNumberIgnoreCase(String registrationNumber);

    boolean existsByRegistrationNumberIgnoreCaseAndIdNot(String registrationNumber, Long id);

    long countByOwnerId(Long ownerId);
}
