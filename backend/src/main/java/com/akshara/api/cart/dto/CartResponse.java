package com.akshara.api.cart.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CartResponse(
        Long cartId,
        List<CartItemResponse> items,
        int distinctItems,
        int totalQuantity,
        BigDecimal totalAmount,
        Instant updatedAt
) {
}