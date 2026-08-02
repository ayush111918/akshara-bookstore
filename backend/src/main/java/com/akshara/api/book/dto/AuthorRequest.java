package com.akshara.api.book.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthorRequest(

        @NotBlank(message = "Author name is required")
        @Size(
                max = 150,
                message = "Author name must not exceed 150 characters"
        )
        String name,

        @Size(
                max = 10000,
                message = "Biography must not exceed 10000 characters"
        )
        String biography
) {
}