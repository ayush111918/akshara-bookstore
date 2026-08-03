package com.akshara.api.user.service;

import com.akshara.api.auth.dto.UserResponse;
import com.akshara.api.auth.exception.InvalidAccessTokenException;
import com.akshara.api.user.entity.AppUser;
import com.akshara.api.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.akshara.api.audit.service.AuditService;
import com.akshara.api.common.exception.InvalidRequestException;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final AccountDataDeletionService accountDataDeletionService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, AuditService auditService,
                       AccountDataDeletionService accountDataDeletionService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.accountDataDeletionService = accountDataDeletionService;
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String subject) {
        return UserResponse.from(getCurrentUserEntity(subject));
    }

    @Transactional(readOnly = true)
    public AppUser getCurrentUserEntity(String subject) {
        Long userId;

        try {
            userId = Long.parseLong(subject);
        } catch (NumberFormatException exception) {
            throw new InvalidAccessTokenException();
        }

        return userRepository.findById(userId)
                .filter(AppUser::isEnabled)
                .orElseThrow(InvalidAccessTokenException::new);
    }

    @Transactional
    public void closeAccount(String subject, String password, String ipAddress, String userAgent) {
        AppUser user = getCurrentUserEntity(subject);
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            auditService.record(user.getId(), user.getEmail(), "ACCOUNT_DELETION", "FAILURE",
                    ipAddress, userAgent, "Password confirmation failed");
            throw new InvalidRequestException("Current password is incorrect");
        }
        Long id = user.getId();
        String email = user.getEmail();
        accountDataDeletionService.purgePrivateData(id);
        user.closeAccount();
        user.setPasswordHash(passwordEncoder.encode(java.util.UUID.randomUUID().toString()));
        userRepository.save(user);
        auditService.record(id, email, "ACCOUNT_DELETION", "SUCCESS", ipAddress, userAgent,
                "Private reader data removed; account disabled and anonymized; order history retained");
    }
}
