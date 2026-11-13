package com.voltx.dto.response;

import com.voltx.enums.AuditAction;

import java.time.LocalDateTime;

public record AuditLogResponse(Long id, String actorEmail, AuditAction action, String entityType, Long entityId,
                               String details, LocalDateTime createdAt) {
}
