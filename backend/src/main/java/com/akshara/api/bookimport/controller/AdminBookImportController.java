package com.akshara.api.bookimport.controller;

import com.akshara.api.book.dto.BookResponse;
import com.akshara.api.bookimport.dto.BookImportRequest;
import com.akshara.api.bookimport.dto.BookImportSource;
import com.akshara.api.bookimport.dto.ExternalBookSearchPage;
import com.akshara.api.bookimport.dto.CatalogueSeedResponse;
import com.akshara.api.bookimport.service.CatalogueSeedService;
import com.akshara.api.bookimport.service.BookImportService;
import com.akshara.api.bookimport.service.ExternalBookSearchService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/book-import")
public class AdminBookImportController {

    private final ExternalBookSearchService searchService;
    private final BookImportService importService;
    private final CatalogueSeedService seedService;

    public AdminBookImportController(
            ExternalBookSearchService searchService,
            BookImportService importService,
            CatalogueSeedService seedService
    ) {
        this.searchService = searchService;
        this.importService = importService;
        this.seedService = seedService;
    }

    @GetMapping("/search")
    public ResponseEntity<ExternalBookSearchPage> search(
            @RequestParam @NotBlank @Size(max = 200) String query,
            @RequestParam(defaultValue = "OPEN_LIBRARY") BookImportSource source,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(20) int size
    ) {
        return ResponseEntity.ok(
                searchService.search(query.trim(), source, page, size)
        );
    }

    @PostMapping
    public ResponseEntity<BookResponse> importBook(
            @Valid @RequestBody BookImportRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(importService.importBook(request));
    }

    @PostMapping("/seed")
    public ResponseEntity<CatalogueSeedResponse> seed(
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int count
    ) {
        return ResponseEntity.ok(seedService.seed(count));
    }
}
