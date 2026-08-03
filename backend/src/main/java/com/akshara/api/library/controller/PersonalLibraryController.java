package com.akshara.api.library.controller;

import com.akshara.api.library.dto.PersonalBookFile;
import com.akshara.api.library.dto.PersonalBookResponse;
import com.akshara.api.library.service.PersonalLibraryService;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/library/uploads")
@Validated
public class PersonalLibraryController {

    private final PersonalLibraryService personalLibraryService;

    public PersonalLibraryController(PersonalLibraryService personalLibraryService) {
        this.personalLibraryService = personalLibraryService;
    }

    @GetMapping
    public List<PersonalBookResponse> getBooks(@AuthenticationPrincipal Jwt jwt) {
        return personalLibraryService.getBooks(jwt.getSubject());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PersonalBookResponse> upload(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam String title,
            @RequestParam(required = false) String author,
            @RequestPart("file") MultipartFile file
    ) {
        return ResponseEntity.status(201).body(personalLibraryService.upload(jwt.getSubject(), title, author, file));
    }

    @GetMapping("/{bookId}/file")
    public ResponseEntity<org.springframework.core.io.Resource> getFile(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable @Positive Long bookId,
            @RequestParam(defaultValue = "inline") String disposition
    ) {
        PersonalBookFile file = personalLibraryService.getFile(jwt.getSubject(), bookId);
        ContentDisposition contentDisposition = "attachment".equalsIgnoreCase(disposition)
                ? ContentDisposition.attachment().filename(file.originalFilename(), StandardCharsets.UTF_8).build()
                : ContentDisposition.inline().filename(file.originalFilename(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.mediaType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(file.resource());
    }

    @DeleteMapping("/{bookId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable @Positive Long bookId
    ) {
        personalLibraryService.delete(jwt.getSubject(), bookId);
        return ResponseEntity.noContent().build();
    }
}
