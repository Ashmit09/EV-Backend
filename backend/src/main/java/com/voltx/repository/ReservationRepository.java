package com.voltx.repository;

import com.voltx.entity.Reservation;
import com.voltx.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    /** Two intervals overlap when each starts before the other ends. */
    @Query("""
            select count(r) > 0 from Reservation r
            where r.chargingUnit.id = :unitId
              and r.status in :statuses
              and r.startTime < :end and r.endTime > :start
            """)
    boolean existsOverlappingOnUnit(@Param("unitId") Long unitId,
                                    @Param("start") LocalDateTime start,
                                    @Param("end") LocalDateTime end,
                                    @Param("statuses") Collection<ReservationStatus> statuses);

    @Query("""
            select count(r) > 0 from Reservation r
            where r.vehicle.id = :vehicleId
              and r.status in :statuses
              and r.startTime < :end and r.endTime > :start
            """)
    boolean existsOverlappingForVehicle(@Param("vehicleId") Long vehicleId,
                                        @Param("start") LocalDateTime start,
                                        @Param("end") LocalDateTime end,
                                        @Param("statuses") Collection<ReservationStatus> statuses);

    @Query("""
            select r from Reservation r
            where r.chargingUnit.id = :unitId
              and r.status in :statuses
              and r.startTime < :end and r.endTime > :start
            order by r.startTime
            """)
    List<Reservation> findOnUnitBetween(@Param("unitId") Long unitId,
                                        @Param("start") LocalDateTime start,
                                        @Param("end") LocalDateTime end,
                                        @Param("statuses") Collection<ReservationStatus> statuses);

    long countByUserIdAndStatusIn(Long userId, Collection<ReservationStatus> statuses);

    @Query("""
            select r from Reservation r
            join fetch r.vehicle join fetch r.chargingUnit c join fetch c.station
            where r.user.id = :userId order by r.startTime desc
            """)
    List<Reservation> findByUserIdWithDetails(@Param("userId") Long userId);

    @Query("""
            select r from Reservation r
            join fetch r.user join fetch r.vehicle join fetch r.chargingUnit c join fetch c.station
            where (:status is null or r.status = :status)
            order by r.startTime desc
            """)
    List<Reservation> findAllWithDetails(@Param("status") ReservationStatus status);

    List<Reservation> findByStatusAndStartTimeLessThanEqualAndEndTimeGreaterThan(
            ReservationStatus status, LocalDateTime startBound, LocalDateTime endBound);

    List<Reservation> findByStatusAndEndTimeLessThanEqual(ReservationStatus status, LocalDateTime now);

    List<Reservation> findByChargingUnitIdAndStatusAndEndTimeAfter(Long unitId, ReservationStatus status,
                                                                    LocalDateTime now);

    List<Reservation> findByChargingUnitStationIdAndStatusAndEndTimeAfter(Long stationId, ReservationStatus status,
                                                                          LocalDateTime now);

    boolean existsByVehicleId(Long vehicleId);

    long countByStartTimeBetween(LocalDateTime start, LocalDateTime end);
}
