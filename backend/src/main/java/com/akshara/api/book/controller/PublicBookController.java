package com.akshara.api.book.controller;

import com.akshara.api.book.dto.BookPageResponse;
import com.akshara.api.book.dto.BookResponse;
import com.akshara.api.book.dto.BookSearchCriteria;
import com.akshara.api.book.dto.BookSort;
import com.akshara.api.book.entity.BookFormat;
import com.akshara.api.book.service.BookService;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.akshara.api.common.exception.InvalidRequestException;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/public/books")
public class PublicBookController {

    private final BookService bookService;

    public PublicBookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    public ResponseEntity<BookPageResponse> search(
            @RequestParam(required = false)
            @Size(
                    max = 200,
                    message = "Search query must not exceed 200 characters"
            )
            String query,

            @RequestParam(required = false)
            @Positive(message = "Author ID must be positive")
            Long authorId,

            @RequestParam(required = false)
            @Positive(message = "Category ID must be positive")
            Long categoryId,

            @RequestParam(required = false)
            @Pattern(
                    regexp = "^[a-z]{2,3}(?:-[A-Z]{2})?$",
                    message = "Language code must use a format such as en, hi or en-US"
            )
            String languageCode,

            @RequestParam(required = false)
            BookFormat format,

            @RequestParam(required = false)
            @DecimalMin(
                    value = "0.00",
                    message = "Minimum price must not be negative"
            )
            BigDecimal minPrice,

            @RequestParam(required = false)
            @DecimalMin(
                    value = "0.00",
                    message = "Maximum price must not be negative"
            )
            BigDecimal maxPrice,

            @RequestParam(defaultValue = "false")
            boolean inStock,

            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "Page number must not be negative")
            int page,

            @RequestParam(defaultValue = "12")
            @Min(value = 1, message = "Page size must be at least 1")
            @Max(value = 100, message = "Page size must not exceed 100")
            int size,

            @RequestParam(defaultValue = "TITLE_ASC")
            BookSort sort
    ) {
        if (minPrice != null
                && maxPrice != null
                && minPrice.compareTo(maxPrice) > 0) {

            throw new InvalidRequestException(
                    "Minimum price must not exceed maximum price"
            );
        }
        BookSearchCriteria criteria = new BookSearchCriteria(
                query,
                authorId,
                categoryId,
                languageCode,
                format,
                minPrice,
                maxPrice,
                inStock
        );

        return ResponseEntity.ok(
                bookService.search(criteria, page, size, sort)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookResponse> getById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(bookService.getById(id));
    }
}