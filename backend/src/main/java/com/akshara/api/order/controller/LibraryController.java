package com.akshara.api.order.controller;

import com.akshara.api.order.dto.LibraryBookResponse;
import com.akshara.api.order.service.OrderQueryService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/library")
public class LibraryController {

    private final OrderQueryService orderQueryService;

    public LibraryController(OrderQueryService orderQueryService) {
        this.orderQueryService = orderQueryService;
    }

    @GetMapping
    public List<LibraryBookResponse> getMyBooks(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return orderQueryService.getLibrary(jwt.getSubject());
    }
}
