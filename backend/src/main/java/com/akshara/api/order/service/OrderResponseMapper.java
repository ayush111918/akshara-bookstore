package com.akshara.api.order.service;

import com.akshara.api.order.dto.CheckoutResponse;
import com.akshara.api.order.dto.OrderItemResponse;
import com.akshara.api.order.dto.ShippingAddressResponse;
import com.akshara.api.order.entity.Address;
import com.akshara.api.order.entity.Order;
import com.akshara.api.order.entity.OrderItem;
import com.akshara.api.order.entity.Payment;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderResponseMapper {

    public CheckoutResponse toCheckoutResponse(
            Order order,
            List<OrderItem> orderItems,
            Payment payment
    ) {
        List<OrderItemResponse> itemResponses = orderItems.stream()
                .map(this::toOrderItemResponse)
                .toList();

        int totalQuantity = itemResponses.stream()
                .mapToInt(OrderItemResponse::quantity)
                .sum();

        return new CheckoutResponse(
                order.getId(),
                order.getStatus(),
                payment.getMethod(),
                payment.getStatus(),
                itemResponses,
                totalQuantity,
                order.getSubtotal(),
                order.getShippingFee(),
                order.getTotalAmount(),
                toShippingAddressResponse(
                        order.getShippingAddress()
                ),
                order.getPlacedAt()
        );
    }

    private OrderItemResponse toOrderItemResponse(
            OrderItem orderItem
    ) {
        return new OrderItemResponse(
                orderItem.getId(),
                orderItem.getBookEdition().getId(),
                orderItem.getBookTitle(),
                orderItem.getCoverImageUrl(),
                orderItem.getBookFormat(),
                orderItem.getEditionName(),
                orderItem.getIsbn10(),
                orderItem.getIsbn13(),
                orderItem.getPublisherName(),
                orderItem.getUnitPrice(),
                orderItem.getQuantity(),
                orderItem.getSubtotal()
        );
    }

    private ShippingAddressResponse toShippingAddressResponse(
            Address address
    ) {
        return new ShippingAddressResponse(
                address.getRecipientName(),
                address.getPhone(),
                address.getAddressLine1(),
                address.getAddressLine2(),
                address.getCity(),
                address.getState(),
                address.getPostalCode(),
                address.getCountry()
        );
    }
}