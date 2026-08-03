package com.akshara.api.bookimport.dto;

import java.util.List;
import java.util.Map;

public record CatalogueSeedResponse(
        int requested,
        int imported,
        int skipped,
        long catalogueSizeBefore,
        long catalogueSizeAfter,
        Map<String, Integer> genreCounts,
        List<String> importedTitles
) {
}
