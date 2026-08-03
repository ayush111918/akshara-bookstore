package com.akshara.api.order.dto;

import com.akshara.api.order.entity.OrderStatus;
import com.akshara.api.order.entity.PaymentMethod;
import com.akshara.api.order.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CheckoutResponse(
        Long orderId,
        OrderStatus orderStatus,
        PaymentMethod paymentMethod,
        PaymentStatus paymentStatus,
        List<OrderItemResponse> items,
        int totalQuantity,
        BigDecimal subtotal,
        BigDecimal shippingFee,
        BigDecimal totalAmount,
        ShippingAddressResponse shippingAddress,
        Instant placedAt
) {
}