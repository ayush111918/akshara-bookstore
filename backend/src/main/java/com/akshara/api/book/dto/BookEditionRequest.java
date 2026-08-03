package com.akshara.api.book.dto;

import com.akshara.api.book.entity.BookFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record BookEditionRequest(

        @Positive(message = "Publisher ID must be positive")
        Long publisherId,

        @NotNull(message = "Book format is required")
        BookFormat format,

        @Size(
                max = 100,
                message = "Edition name must not exceed 100 characters"
        )
        String editionName,

        @Size(max = 20, message = "ISBN-10 input must not exceed 20 characters")
        String isbn10,

        @Size(max = 25, message = "ISBN-13 input must not exceed 25 characters")
        String isbn13,

        @PastOrPresent(
                message = "Publication date cannot be in the future"
        )
        LocalDate publicationDate,

        @Positive(message = "Page count must be greater than zero")
        Integer pageCount,

        @NotBlank(message = "SKU is required")
        @Size(max = 100, message = "SKU must not exceed 100 characters")
        String sku,

        @NotNull(message = "Inventory information is required")
        @Valid
        InventoryRequest inventory
) {
}
