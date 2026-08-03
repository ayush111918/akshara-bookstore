package com.akshara.api.book.controller;

import com.akshara.api.book.dto.BookRequest;
import com.akshara.api.book.dto.BookResponse;
import com.akshara.api.book.dto.FeaturedRequest;
import com.akshara.api.book.service.BookService;
import com.akshara.api.bookimport.dto.ManualBookRequest;
import com.akshara.api.bookimport.service.BookImportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/books")
public class AdminBookController {

    private final BookService bookService;
    private final BookImportService bookImportService;

    public AdminBookController(
            BookService bookService,
            BookImportService bookImportService
    ) {
        this.bookService = bookService;
        this.bookImportService = bookImportService;
    }

    @PostMapping
    public ResponseEntity<BookResponse> create(
            @Valid @RequestBody ManualBookRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(bookImportService.createManualBook(request));
    }

    @GetMapping
    public ResponseEntity<List<BookResponse>> getAll() {
        return ResponseEntity.ok(bookService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookResponse> getById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(bookService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BookResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody BookRequest request
    ) {
        return ResponseEntity.ok(
                bookService.update(id, request)
        );
    }

    @PatchMapping("/{id}/featured")
    public ResponseEntity<BookResponse> updateFeatured(
            @PathVariable Long id,
            @Valid @RequestBody FeaturedRequest request
    ) {
        return ResponseEntity.ok(
                bookService.updateFeatured(id, request.featured())
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        bookService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
