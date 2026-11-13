package com.voltx.service;

import com.voltx.dto.response.AuditLogResponse;
import com.voltx.dto.response.PageResponse;
import com.voltx.enums.AuditAction;

public interface AuditService {

    void record(String actorEmail, AuditAction action, String entityType, Long entityId, String details);

    /** Overload for events that are not about one specific entity. */
    void record(String actorEmail, AuditAction action, String details);

    PageResponse<AuditLogResponse> list(AuditAction action, int page, int size);
}
