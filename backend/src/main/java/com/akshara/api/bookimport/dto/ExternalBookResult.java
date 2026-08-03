package com.akshara.api.bookimport.dto;

import java.util.List;

public record ExternalBookResult(
        BookImportSource source,
        String sourceId,
        String editionId,
        String title,
        String subtitle,
        String description,
        List<String> authors,
        String publisher,
        String languageCode,
        Integer pageCount,
        List<String> categories,
        String coverImageUrl,
        String publicationDate,
        String isbn10,
        String isbn13,
        String editionName,
        boolean alreadyImported
) {
    public ExternalBookResult withAlreadyImported(boolean imported) {
        return new ExternalBookResult(
                source, sourceId, editionId, title, subtitle, description,
                authors, publisher, languageCode, pageCount, categories,
                coverImageUrl, publicationDate, isbn10, isbn13, editionName,
                imported
        );
    }
}
