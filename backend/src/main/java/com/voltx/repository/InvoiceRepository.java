package com.voltx.repository;

import com.voltx.entity.Invoice;
import com.voltx.enums.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    @Query("""
            select i from Invoice i
            join fetch i.session s join fetch s.chargingUnit c join fetch c.station join fetch s.vehicle
            where i.user.id = :userId order by i.issuedAt desc
            """)
    List<Invoice> findByUserIdWithDetails(@Param("userId") Long userId);

    @Query("""
            select i from Invoice i
            join fetch i.user join fetch i.session s join fetch s.chargingUnit c join fetch c.station
            join fetch s.vehicle
            order by i.issuedAt desc
            """)
    List<Invoice> findAllWithDetails();

    Optional<Invoice> findBySessionId(Long sessionId);

    @Query("select coalesce(sum(i.totalAmount), 0) from Invoice i")
    BigDecimal sumTotalAmount();

    @Query("select coalesce(sum(i.totalAmount), 0) from Invoice i where i.status = :status")
    BigDecimal sumTotalAmountByStatus(@Param("status") InvoiceStatus status);
}
