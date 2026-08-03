package com.akshara.api.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record CheckoutRequest(

        @NotNull(message = "Shipping address is required")
        @Valid
        ShippingAddressRequest shippingAddress
) {
}