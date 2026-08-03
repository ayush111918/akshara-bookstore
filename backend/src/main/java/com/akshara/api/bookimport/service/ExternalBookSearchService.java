package com.akshara.api.bookimport.service;

import com.akshara.api.book.repository.BookEditionRepository;
import com.akshara.api.bookimport.dto.BookImportSource;
import com.akshara.api.bookimport.dto.ExternalBookResult;
import com.akshara.api.bookimport.dto.ExternalBookSearchPage;
import com.akshara.api.bookimport.dto.ExternalBookSearchQuery;
import com.akshara.api.bookimport.provider.BookMetadataProvider;
import com.akshara.api.common.exception.InvalidRequestException;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ExternalBookSearchService {

    private final Map<BookImportSource, BookMetadataProvider> providers;
    private final BookEditionRepository editionRepository;

    public ExternalBookSearchService(
            List<BookMetadataProvider> providers,
            BookEditionRepository editionRepository
    ) {
        this.providers = providers.stream().collect(Collectors.toMap(
                BookMetadataProvider::source,
                Function.identity(),
                (first, ignored) -> first,
                () -> new EnumMap<>(BookImportSource.class)
        ));
        this.editionRepository = editionRepository;
    }

    public ExternalBookSearchPage search(
            String query,
            BookImportSource source,
            int page,
            int size
    ) {
        BookMetadataProvider provider = providers.get(source);
        if (provider == null) {
            throw new InvalidRequestException(
                    source + " is not configured for this Akshara installation"
            );
        }
        ExternalBookSearchPage result = provider.search(
                new ExternalBookSearchQuery(query, page, size)
        );
        List<ExternalBookResult> annotated = result.results().stream()
                .map(item -> item.withAlreadyImported(alreadyImported(item)))
                .toList();
        return new ExternalBookSearchPage(
                result.source(), annotated, result.page(), result.size(),
                result.totalResults()
        );
    }

    private boolean alreadyImported(ExternalBookResult result) {
        return (result.isbn10() != null
                && editionRepository.existsByIsbn10(result.isbn10()))
                || (result.isbn13() != null
                && editionRepository.existsByIsbn13(result.isbn13()));
    }
}
