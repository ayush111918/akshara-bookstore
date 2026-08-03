package com.akshara.api.reading.controller;

import com.akshara.api.reading.dto.*;
import com.akshara.api.reading.service.ReadingJourneyService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reading-journey")
@Validated
public class ReadingJourneyController {

    private final ReadingJourneyService readingJourneyService;

    public ReadingJourneyController(ReadingJourneyService readingJourneyService) {
        this.readingJourneyService = readingJourneyService;
    }

    @GetMapping
    public ReadingDashboardResponse getDashboard(@AuthenticationPrincipal Jwt jwt) {
        return readingJourneyService.getDashboard(jwt.getSubject());
    }

    @PostMapping
    public ResponseEntity<ReadingEntryResponse> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateReadingEntryRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(readingJourneyService.create(jwt.getSubject(), request));
    }

    @PutMapping("/{entryId}/progress")
    public ReadingEntryResponse updateProgress(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable @Positive Long entryId,
            @Valid @RequestBody UpdateReadingProgressRequest request
    ) {
        return readingJourneyService.updateProgress(jwt.getSubject(), entryId, request);
    }

    @DeleteMapping("/{entryId}")
    public ResponseEntity<Void> deleteEntry(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable @Positive Long entryId
    ) {
        readingJourneyService.deleteEntry(jwt.getSubject(), entryId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{entryId}/annotations")
    public List<ReadingAnnotationResponse> getAnnotations(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable @Positive Long entryId
    ) {
        return readingJourneyService.getAnnotations(jwt.getSubject(), entryId);
    }

    @PostMapping("/{entryId}/annotations")
    public ResponseEntity<ReadingAnnotationResponse> createAnnotation(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable @Positive Long entryId,
            @Valid @RequestBody ReadingAnnotationRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                readingJourneyService.createAnnotation(jwt.getSubject(), entryId, request)
        );
    }

    @PutMapping("/annotations/{annotationId}")
    public ReadingAnnotationResponse updateAnnotation(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable @Positive Long annotationId,
            @Valid @RequestBody ReadingAnnotationRequest request
    ) {
        return readingJourneyService.updateAnnotation(
                jwt.getSubject(), annotationId, request
        );
    }

    @DeleteMapping("/annotations/{annotationId}")
    public ResponseEntity<Void> deleteAnnotation(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable @Positive Long annotationId
    ) {
        readingJourneyService.deleteAnnotation(jwt.getSubject(), annotationId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/goal/{year}")
    public ReadingGoalResponse updateGoal(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable @Min(2000) @Max(2100) int year,
            @Valid @RequestBody ReadingGoalRequest request
    ) {
        return readingJourneyService.updateGoal(jwt.getSubject(), year, request);
    }
}
