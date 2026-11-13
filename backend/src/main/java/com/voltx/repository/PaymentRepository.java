package com.voltx.repository;

import com.voltx.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByInvoiceIdOrderByProcessedAtDesc(Long invoiceId);

    @Query("select p from Payment p join fetch p.invoice i join fetch i.user order by p.processedAt desc")
    List<Payment> findAllWithInvoice();
}
