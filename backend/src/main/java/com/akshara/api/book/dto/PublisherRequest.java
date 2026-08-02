package com.akshara.api.book.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PublisherRequest(

        @NotBlank(message = "Publisher name is required")
        @Size(
                max = 150,
                message = "Publisher name must not exceed 150 characters"
        )
        String name,

        @Size(
                max = 1000,
                message = "Website URL must not exceed 1000 characters"
        )
        @Pattern(
                regexp = "^$|^https?://.+$",
                message = "Website URL must begin with http:// or https://"
        )
        String websiteUrl
) {
}