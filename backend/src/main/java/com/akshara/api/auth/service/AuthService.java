package com.akshara.api.auth.service;

import com.akshara.api.auth.dto.AuthResponse;
import com.akshara.api.auth.dto.RegisterRequest;
import com.akshara.api.auth.dto.UserResponse;
import com.akshara.api.auth.exception.EmailAlreadyExistsException;
import com.akshara.api.user.entity.AppUser;
import com.akshara.api.user.entity.Role;
import com.akshara.api.user.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.akshara.api.auth.dto.LoginRequest;
import com.akshara.api.auth.exception.InvalidCredentialsException;

import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedName = request.fullName().trim();
        String normalizedEmail = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new EmailAlreadyExistsException();
        }

        AppUser user = new AppUser(
                normalizedName,
                normalizedEmail,
                passwordEncoder.encode(request.password()),
                Role.READER
        );

        AppUser savedUser;

        try {
            savedUser = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw new EmailAlreadyExistsException();
        }

        JwtService.GeneratedToken token =
                jwtService.generateToken(savedUser);

        return new AuthResponse(
                token.value(),
                "Bearer",
                token.expiresIn(),
                UserResponse.from(savedUser)
        );
    }
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        AppUser user = userRepository
                .findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(InvalidCredentialsException::new);

        if (!user.isEnabled()
                || !passwordEncoder.matches(
                request.password(),
                user.getPasswordHash()
        )) {
            throw new InvalidCredentialsException();
        }

        JwtService.GeneratedToken token =
                jwtService.generateToken(user);

        return new AuthResponse(
                token.value(),
                "Bearer",
                token.expiresIn(),
                UserResponse.from(user)
        );
    }
}