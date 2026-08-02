package com.akshara.api.order.dto;

import com.akshara.api.order.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderSummaryResponse(
        Long orderId,
        OrderStatus orderStatus,
        BigDecimal totalAmount,
        Instant placedAt
) {
}