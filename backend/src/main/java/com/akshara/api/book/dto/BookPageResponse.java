package com.akshara.api.book.dto;

import java.util.List;

public record BookPageResponse(
        List<BookResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
}