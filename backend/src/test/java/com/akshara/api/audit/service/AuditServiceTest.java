package com.akshara.api.audit.service;

import com.akshara.api.audit.entity.AuditLog;
import com.akshara.api.audit.event.AuditRecordRequested;
import com.akshara.api.audit.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock AuditLogRepository repository;
    @Mock ApplicationEventPublisher publisher;

    @Test
    void successfulAuditIsPublishedForAfterCommitHandling() {
        AuditService service = new AuditService(repository, publisher);

        service.recordAfterCommit(7L, " reader@example.com ", "LOGIN", "SUCCESS",
                " 127.0.0.1 ", "browser", "signed in");

        verify(publisher).publishEvent(new AuditRecordRequested(
                7L, "reader@example.com", "LOGIN", "SUCCESS",
                "127.0.0.1", "browser", "signed in"
        ));
    }

    @Test
    void committedAuditListenerPersistsTheRequestedRecord() {
        AuditRecordListener listener = new AuditRecordListener(repository);
        listener.recordCommittedSuccess(new AuditRecordRequested(
                7L, "reader@example.com", "LOGIN", "SUCCESS",
                "127.0.0.1", "browser", "signed in"
        ));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo("LOGIN");
        assertThat(captor.getValue().getOutcome()).isEqualTo("SUCCESS");
    }
}
