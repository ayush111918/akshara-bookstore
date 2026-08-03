package com.akshara.api.audit.event;

public record AuditRecordRequested(
        Long userId,
        String email,
        String action,
        String outcome,
        String ipAddress,
        String userAgent,
        String details
) {
}
