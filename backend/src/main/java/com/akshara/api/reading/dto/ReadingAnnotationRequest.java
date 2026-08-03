package com.akshara.api.reading.dto;

import com.akshara.api.reading.entity.ReadingAnnotationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ReadingAnnotationRequest(
        @NotNull ReadingAnnotationType type,
        @NotBlank @Size(max = 3000) String content,
        @Positive Integer pageNumber
) {
}
