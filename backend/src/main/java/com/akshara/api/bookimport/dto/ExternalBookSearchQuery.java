package com.akshara.api.bookimport.dto;

public record ExternalBookSearchQuery(
        String query,
        int page,
        int size
) {
}
