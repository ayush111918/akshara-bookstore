package com.akshara.api.bookimport.dto;

import com.akshara.api.book.entity.AvailabilityStatus;
import com.akshara.api.book.entity.BookFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ManualBookRequest(
        @NotBlank @Size(max = 255) String title,
        @Size(max = 255) String subtitle,
        @Size(max = 20000) String description,
        @NotEmpty List<@NotBlank @Size(max = 150) String> authors,
        @Size(max = 150) String publisher,
        @Pattern(regexp = "^$|^[a-z]{2,3}(?:-[A-Z]{2})?$") @Size(max = 10)
        String languageCode,
        @Positive Integer pageCount,
        @NotEmpty List<@NotBlank @Size(max = 100) String> categories,
        @Pattern(regexp = "^$|^https?://.+$") @Size(max = 1000) String coverImageUrl,
        @PastOrPresent LocalDate publicationDate,
        @Size(max = 100) String editionName,
        @Size(max = 20) String isbn10,
        @Size(max = 25) String isbn13,
        @NotNull BookFormat format,
        @NotNull @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal price,
        @NotNull @PositiveOrZero Integer stockQuantity,
        @NotNull AvailabilityStatus availabilityStatus,
        @NotBlank @Size(max = 100) String sku,
        @NotNull Boolean active,
        @NotNull Boolean featured
) implements CatalogueBookCommand {
}
