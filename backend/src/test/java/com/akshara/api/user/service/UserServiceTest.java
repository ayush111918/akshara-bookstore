package com.akshara.api.user.service;

import com.akshara.api.audit.service.AuditService;
import com.akshara.api.common.exception.InvalidRequestException;
import com.akshara.api.user.entity.AppUser;
import com.akshara.api.user.entity.Role;
import com.akshara.api.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock UserRepository repository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AuditService auditService;
    @Mock AccountDataDeletionService accountDataDeletionService;
    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(repository, passwordEncoder, auditService, accountDataDeletionService);
    }

    @Test
    void closeAccountShouldRequireTheCurrentPassword() {
        AppUser user = user();
        when(repository.findById(7L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        assertThatThrownBy(() -> service.closeAccount("7", "wrong", "127.0.0.1", "test"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Current password is incorrect");

        assertThat(user.isEnabled()).isTrue();
        verify(repository, never()).save(any());
        verifyNoInteractions(accountDataDeletionService);
        verify(auditService).record(7L, "reader@example.com", "ACCOUNT_DELETION", "FAILURE",
                "127.0.0.1", "test", "Password confirmation failed");
    }

    @Test
    void closeAccountShouldDisableAndAnonymizeIdentity() {
        AppUser user = user();
        when(repository.findById(7L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret123", "hash")).thenReturn(true);
        when(passwordEncoder.encode(anyString())).thenReturn("irreversible-hash");

        service.closeAccount("7", "secret123", "127.0.0.1", "test");

        assertThat(user.isEnabled()).isFalse();
        assertThat(user.getFullName()).isEqualTo("Deleted Reader");
        assertThat(user.getEmail()).startsWith("deleted-7-");
        assertThat(user.getPasswordHash()).isEqualTo("irreversible-hash");
        assertThat(user.getDeletedAt()).isNotNull();
        verify(accountDataDeletionService).purgePrivateData(7L);
        verify(repository).save(user);
        verify(auditService).recordAfterCommit(7L, "reader@example.com", "ACCOUNT_DELETION", "SUCCESS",
                "127.0.0.1", "test",
                "Private reader data removed; account disabled and anonymized; order history retained");
    }

    private AppUser user() {
        AppUser user = new AppUser("Reader", "reader@example.com", "hash", Role.READER);
        ReflectionTestUtils.setField(user, "id", 7L);
        return user;
    }
}
