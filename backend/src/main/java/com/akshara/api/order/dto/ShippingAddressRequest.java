package com.akshara.api.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ShippingAddressRequest(

        @NotBlank(message = "Recipient name is required")
        @Size(
                max = 150,
                message = "Recipient name must not exceed 150 characters"
        )
        String recipientName,

        @NotBlank(message = "Phone number is required")
        @Size(
                max = 20,
                message = "Phone number must not exceed 20 characters"
        )
        @Pattern(
                regexp = "^\\+?[0-9][0-9 -]{6,19}$",
                message = "Phone number contains invalid characters"
        )
        String phone,

        @NotBlank(message = "Address line 1 is required")
        @Size(
                max = 255,
                message = "Address line 1 must not exceed 255 characters"
        )
        String addressLine1,

        @Size(
                max = 255,
                message = "Address line 2 must not exceed 255 characters"
        )
        String addressLine2,

        @NotBlank(message = "City is required")
        @Size(
                max = 100,
                message = "City must not exceed 100 characters"
        )
        String city,

        @NotBlank(message = "State is required")
        @Size(
                max = 100,
                message = "State must not exceed 100 characters"
        )
        String state,

        @NotBlank(message = "Postal code is required")
        @Size(
                max = 20,
                message = "Postal code must not exceed 20 characters"
        )
        @Pattern(
                regexp = "^[A-Za-z0-9][A-Za-z0-9 -]{2,19}$",
                message = "Postal code contains invalid characters"
        )
        String postalCode,

        @NotBlank(message = "Country is required")
        @Size(
                max = 100,
                message = "Country must not exceed 100 characters"
        )
        String country
) {
}