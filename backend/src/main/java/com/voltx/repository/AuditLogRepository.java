package com.voltx.repository;

import com.voltx.entity.AuditLog;
import com.voltx.enums.AuditAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);

    Page<AuditLog> findByActionOrderByCreatedAtDescIdDesc(AuditAction action, Pageable pageable);
}
