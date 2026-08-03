package com.akshara.api.audit.dto;

import com.akshara.api.audit.entity.AuditLog;

import java.time.Instant;

public record AuditLogResponse(
        Long id, Long actorUserId, String actorEmail, String action, String outcome,
        String ipAddress, String userAgent, String details, Instant createdAt
) {
    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(log.getId(), log.getActorUserId(), log.getActorEmail(),
                log.getAction(), log.getOutcome(), log.getIpAddress(), log.getUserAgent(),
                log.getDetails(), log.getCreatedAt());
    }
}
