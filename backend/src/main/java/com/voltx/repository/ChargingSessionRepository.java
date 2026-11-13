package com.voltx.repository;

import com.voltx.entity.ChargingSession;
import com.voltx.enums.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ChargingSessionRepository extends JpaRepository<ChargingSession, Long> {

    Optional<ChargingSession> findFirstByUserIdAndStatusOrderByStartTimeDesc(Long userId, SessionStatus status);

    @Query("""
            select s from ChargingSession s
            join fetch s.vehicle join fetch s.chargingUnit c join fetch c.station
            where s.user.id = :userId order by s.startTime desc
            """)
    List<ChargingSession> findByUserIdWithDetails(@Param("userId") Long userId);

    @Query("""
            select s from ChargingSession s
            join fetch s.user join fetch s.vehicle join fetch s.chargingUnit c join fetch c.station
            where s.status = :status order by s.startTime desc
            """)
    List<ChargingSession> findByStatusWithDetails(@Param("status") SessionStatus status);

    List<ChargingSession> findByStatus(SessionStatus status);

    boolean existsByVehicleIdAndStatus(Long vehicleId, SessionStatus status);

    boolean existsByVehicleId(Long vehicleId);

    boolean existsByChargingUnitStationIdAndStatus(Long stationId, SessionStatus status);

    long countByStatus(SessionStatus status);

    @Query("select coalesce(sum(s.energyConsumedKwh), 0) from ChargingSession s where s.status = :status")
    BigDecimal sumEnergyByStatus(@Param("status") SessionStatus status);
}
