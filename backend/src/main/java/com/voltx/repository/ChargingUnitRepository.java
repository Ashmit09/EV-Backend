package com.voltx.repository;

import com.voltx.entity.ChargingUnit;
import com.voltx.enums.ChargerStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChargingUnitRepository extends JpaRepository<ChargingUnit, Long> {

    List<ChargingUnit> findByStationIdOrderByChargerCode(Long stationId);

    @Query("select c from ChargingUnit c join fetch c.station order by c.station.city, c.chargerCode")
    List<ChargingUnit> findAllWithStation();

    /**
     * SELECT ... FOR UPDATE on the charger row. Booking and session start take this lock first,
     * so two concurrent requests for the same charger are serialised by the database.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ChargingUnit c where c.id = :id")
    Optional<ChargingUnit> findByIdForUpdate(@Param("id") Long id);

    boolean existsByChargerCodeIgnoreCase(String chargerCode);

    long countByStatus(ChargerStatus status);

    @Query("select c.status as status, count(c) as total from ChargingUnit c group by c.status")
    List<StatusCount> countGroupedByStatus();

    interface StatusCount {
        ChargerStatus getStatus();

        long getTotal();
    }
}
