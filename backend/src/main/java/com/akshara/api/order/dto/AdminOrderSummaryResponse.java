package com.akshara.api.order.dto;

import com.akshara.api.order.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record AdminOrderSummaryResponse(
        Long orderId,
        Long userId,
        String customerName,
        String customerEmail,
        OrderStatus orderStatus,
        BigDecimal totalAmount,
        Instant placedAt,
        Instant updatedAt
) {
}