package com.akshara.api.book.dto;

import com.akshara.api.book.entity.BookFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
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

        @Pattern(
                regexp = "^$|^[0-9]{9}[0-9Xx]$",
                message = "ISBN-10 must contain 10 valid characters"
        )
        String isbn10,

        @Pattern(
                regexp = "^$|^[0-9]{13}$",
                message = "ISBN-13 must contain exactly 13 digits"
        )
        String isbn13,

        @PastOrPresent(
                message = "Publication date cannot be in the future"
        )
        LocalDate publicationDate,

        @Positive(message = "Page count must be greater than zero")
        Integer pageCount,

        @NotNull(message = "Inventory information is required")
        @Valid
        InventoryRequest inventory
) {
}