package com.akshara.api.bookimport.dto;

import java.util.List;

public record ExternalBookSearchPage(
        BookImportSource source,
        List<ExternalBookResult> results,
        int page,
        int size,
        long totalResults
) {
}
