package com.akshara.api.order.controller;

import com.akshara.api.order.dto.CheckoutResponse;
import com.akshara.api.order.dto.OrderSummaryResponse;
import com.akshara.api.order.service.OrderQueryService;
import jakarta.validation.constraints.Positive;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@Validated
public class OrderController {

    private final OrderQueryService orderQueryService;

    public OrderController(
            OrderQueryService orderQueryService
    ) {
        this.orderQueryService = orderQueryService;
    }

    @GetMapping
    public List<OrderSummaryResponse> getOrders(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return orderQueryService.getOrders(
                jwt.getSubject()
        );
    }

    @GetMapping("/{orderId}")
    public CheckoutResponse getOrder(
            @AuthenticationPrincipal Jwt jwt,

            @PathVariable
            @Positive(message = "Order ID must be positive")
            Long orderId
    ) {
        return orderQueryService.getOrder(
                jwt.getSubject(),
                orderId
        );
    }
}