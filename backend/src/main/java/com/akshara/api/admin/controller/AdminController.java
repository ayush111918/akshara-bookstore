package com.akshara.api.admin.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> getAdminHealth() {
        return ResponseEntity.ok(
                Map.of(
                        "status", "UP",
                        "access", "ADMIN"
                )
        );
    }
}