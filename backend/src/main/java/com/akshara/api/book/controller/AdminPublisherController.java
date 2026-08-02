package com.akshara.api.book.controller;

import com.akshara.api.book.dto.PublisherRequest;
import com.akshara.api.book.dto.PublisherResponse;
import com.akshara.api.book.service.PublisherService;
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
@RequestMapping("/api/admin/publishers")
public class AdminPublisherController {

    private final PublisherService publisherService;

    public AdminPublisherController(PublisherService publisherService) {
        this.publisherService = publisherService;
    }

    @PostMapping
    public ResponseEntity<PublisherResponse> create(
            @Valid @RequestBody PublisherRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(publisherService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<PublisherResponse>> getAll() {
        return ResponseEntity.ok(publisherService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PublisherResponse> getById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(publisherService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PublisherResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody PublisherRequest request
    ) {
        return ResponseEntity.ok(
                publisherService.update(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        publisherService.delete(id);
        return ResponseEntity.noContent().build();
    }
}