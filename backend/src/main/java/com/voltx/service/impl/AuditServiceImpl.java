package com.voltx.service.impl;

import com.voltx.dto.response.AuditLogResponse;
import com.voltx.dto.response.PageResponse;
import com.voltx.entity.AuditLog;
import com.voltx.enums.AuditAction;
import com.voltx.repository.AuditLogRepository;
import com.voltx.service.AuditService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class AuditServiceImpl implements AuditService {

    private static final int MAX_PAGE_SIZE = 100;

    private final AuditLogRepository auditLogRepository;
    private final Clock clock;

    public AuditServiceImpl(AuditLogRepository auditLogRepository, Clock clock) {
        this.auditLogRepository = auditLogRepository;
        this.clock = clock;
    }

    /** Joins the caller's transaction, so the audit row is saved only if the business change commits. */
    @Override
    @Transactional
    public void record(String actorEmail, AuditAction action, String entityType, Long entityId, String details) {
        auditLogRepository.save(new AuditLog(actorEmail, action, entityType, entityId, details,
                LocalDateTime.now(clock)));
    }

    @Override
    @Transactional
    public void record(String actorEmail, AuditAction action, String details) {
        record(actorEmail, action, "SYSTEM", null, details);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> list(AuditAction action, int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
        var logs = action == null
                ? auditLogRepository.findAllByOrderByCreatedAtDescIdDesc(pageable)
                : auditLogRepository.findByActionOrderByCreatedAtDescIdDesc(action, pageable);
        return PageResponse.from(logs, log -> new AuditLogResponse(log.getId(), log.getActorEmail(), log.getAction(),
                log.getEntityType(), log.getEntityId(), log.getDetails(), log.getCreatedAt()));
    }
}
