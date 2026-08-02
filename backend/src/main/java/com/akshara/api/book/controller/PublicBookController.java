package com.akshara.api.book.controller;

import com.akshara.api.book.dto.BookResponse;
import com.akshara.api.book.service.BookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public/books")
public class PublicBookController {

    private final BookService bookService;

    public PublicBookController(BookService bookService) {
        this.bookService = bookService;
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
}