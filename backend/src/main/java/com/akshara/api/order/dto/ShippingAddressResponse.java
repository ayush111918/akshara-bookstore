package com.akshara.api.order.dto;

public record ShippingAddressResponse(
        String recipientName,
        String phone,
        String addressLine1,
        String addressLine2,
        String city,
        String state,
        String postalCode,
        String country
) {
}