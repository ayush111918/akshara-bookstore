package com.akshara.api.auth.service;

import com.akshara.api.audit.service.AuditService;
import com.akshara.api.auth.dto.LoginRequest;
import com.akshara.api.auth.exception.InvalidCredentialsException;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock AuditService auditService;
    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(userRepository, passwordEncoder, jwtService, auditService);
    }

    @Test
    void loginShouldIssueTokenAndAuditSuccess() {
        AppUser user = user();
        when(userRepository.findByEmailIgnoreCase("reader@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret123", "hash")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn(new JwtService.GeneratedToken("jwt", 3600));

        var response = service.login(new LoginRequest(" Reader@Example.com ", "secret123"), "127.0.0.1", "test");

        assertThat(response.accessToken()).isEqualTo("jwt");
        verify(auditService).recordAfterCommit(7L, "reader@example.com", "LOGIN", "SUCCESS",
                "127.0.0.1", "test", "Reader signed in");
    }

    @Test
    void loginShouldUseOneGenericFailureAndAuditIt() {
        when(userRepository.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(
                new LoginRequest("missing@example.com", "wrong-pass"), "10.0.0.2", "test"
        )).isInstanceOf(InvalidCredentialsException.class);

        verify(auditService).record(null, "missing@example.com", "LOGIN", "FAILURE",
                "10.0.0.2", "test", "Invalid credentials");
        verify(jwtService, never()).generateToken(any());
    }

    private AppUser user() {
        AppUser user = new AppUser("Reader", "reader@example.com", "hash", Role.READER);
        ReflectionTestUtils.setField(user, "id", 7L);
        return user;
    }
}
