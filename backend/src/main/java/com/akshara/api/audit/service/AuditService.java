package com.akshara.api.audit.service;

import com.akshara.api.audit.dto.AuditLogResponse;
import com.akshara.api.audit.entity.AuditLog;
import com.akshara.api.audit.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {
    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Long userId, String email, String action, String outcome,
                       String ipAddress, String userAgent, String details) {
        repository.save(new AuditLog(userId, trim(email, 254), action, outcome,
                trim(ipAddress, 64), trim(userAgent, 500), trim(details, 1000)));
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getRecent(int page, int size) {
        return repository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size))
                .map(AuditLogResponse::from);
    }

    private String trim(String value, int max) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim();
        return normalized.length() <= max ? normalized : normalized.substring(0, max);
    }
}
