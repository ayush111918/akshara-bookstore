package com.akshara.api.user.controller;

import com.akshara.api.auth.dto.UserResponse;
import com.akshara.api.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import com.akshara.api.user.dto.DeleteAccountRequest;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(
                userService.getCurrentUser(jwt.getSubject())
        );
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteCurrentAccount(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody DeleteAccountRequest request,
            HttpServletRequest httpRequest
    ) {
        String forwarded = httpRequest.getHeader("X-Forwarded-For");
        String ip = forwarded == null || forwarded.isBlank()
                ? httpRequest.getRemoteAddr() : forwarded.split(",", 2)[0].trim();
        userService.closeAccount(jwt.getSubject(), request.password(), ip, httpRequest.getHeader("User-Agent"));
        return ResponseEntity.noContent().build();
    }
}
