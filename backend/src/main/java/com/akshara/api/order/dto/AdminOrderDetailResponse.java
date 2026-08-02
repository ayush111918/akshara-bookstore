package com.akshara.api.order.dto;

public record AdminOrderDetailResponse(
        Long userId,
        String customerName,
        String customerEmail,
        CheckoutResponse order
) {
}