package com.voltx.repository;

import com.voltx.entity.ChargingStation;
import com.voltx.enums.StationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Abstraction over station persistence. Services depend on this interface; Spring Data
 * generates the implementation at runtime, so no SQL lives in the service layer.
 */
public interface StationRepository extends JpaRepository<ChargingStation, Long> {

    @Query("""
            select distinct s from ChargingStation s
            left join fetch s.chargingUnits
            where (:city is null or lower(s.city) = lower(:city))
              and (:keyword is null
                   or lower(s.name) like lower(concat('%', :keyword, '%'))
                   or lower(s.address) like lower(concat('%', :keyword, '%'))
                   or lower(s.city) like lower(concat('%', :keyword, '%')))
              and (:status is null or s.status = :status)
            order by s.city, s.name
            """)
    List<ChargingStation> search(@Param("city") String city,
                                 @Param("keyword") String keyword,
                                 @Param("status") StationStatus status);

    @Query("select distinct s.city from ChargingStation s where s.status = :status order by s.city")
    List<String> findDistinctCities(@Param("status") StationStatus status);

    boolean existsByNameIgnoreCaseAndCityIgnoreCase(String name, String city);

    boolean existsByNameIgnoreCaseAndCityIgnoreCaseAndIdNot(String name, String city, Long id);

    long countByStatus(StationStatus status);
}
