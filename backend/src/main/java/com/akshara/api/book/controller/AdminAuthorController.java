package com.akshara.api.book.controller;

import com.akshara.api.book.dto.AuthorRequest;
import com.akshara.api.book.dto.AuthorResponse;
import com.akshara.api.book.service.AuthorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/authors")
public class AdminAuthorController {

    private final AuthorService authorService;

    public AdminAuthorController(AuthorService authorService) {
        this.authorService = authorService;
    }

    @PostMapping
    public ResponseEntity<AuthorResponse> create(
            @Valid @RequestBody AuthorRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(authorService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<AuthorResponse>> getAll() {
        return ResponseEntity.ok(authorService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuthorResponse> getById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(authorService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AuthorResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody AuthorRequest request
    ) {
        return ResponseEntity.ok(authorService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        authorService.delete(id);
        return ResponseEntity.noContent().build();
    }
}