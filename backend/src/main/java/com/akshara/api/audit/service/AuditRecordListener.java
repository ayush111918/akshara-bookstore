package com.akshara.api.audit.service;

import com.akshara.api.audit.entity.AuditLog;
import com.akshara.api.audit.event.AuditRecordRequested;
import com.akshara.api.audit.repository.AuditLogRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class AuditRecordListener {

    private final AuditLogRepository repository;

    public AuditRecordListener(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true
    )
    public void recordCommittedSuccess(AuditRecordRequested event) {
        repository.save(new AuditLog(
                event.userId(), event.email(), event.action(), event.outcome(),
                event.ipAddress(), event.userAgent(), event.details()
        ));
    }
}
