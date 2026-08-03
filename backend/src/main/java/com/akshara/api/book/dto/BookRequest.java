package com.akshara.api.book.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Set;

public record BookRequest(

        @NotBlank(message = "Book title is required")
        @Size(
                max = 255,
                message = "Book title must not exceed 255 characters"
        )
        String title,

        @Size(
                max = 255,
                message = "Subtitle must not exceed 255 characters"
        )
        String subtitle,

        @Size(
                max = 20000,
                message = "Description must not exceed 20000 characters"
        )
        String description,

        @Size(
                max = 1000,
                message = "Cover image URL must not exceed 1000 characters"
        )
        @Pattern(
                regexp = "^$|^https?://.+$",
                message = "Cover image URL must begin with http:// or https://"
        )
        String coverImageUrl,

        @Size(
                max = 10,
                message = "Language code must not exceed 10 characters"
        )
        @Pattern(
                regexp = "^$|^[a-z]{2,3}(?:-[A-Z]{2})?$",
                message = "Language code must use a format such as en, hi or en-US"
        )
        String languageCode,

        @NotNull(message = "Featured status is required")
        Boolean featured,

        @NotEmpty(message = "At least one author is required")
        Set<
                @NotNull(message = "Author ID must not be null")
                @Positive(message = "Author ID must be positive")
                        Long
                > authorIds,

        @NotEmpty(message = "At least one category is required")
        Set<
                @NotNull(message = "Category ID must not be null")
                @Positive(message = "Category ID must be positive")
                        Long
                > categoryIds,

        @NotEmpty(message = "At least one book edition is required")
        @Valid
        List<BookEditionRequest> editions
) {
}
