package com.akshara.api.bookimport.service;

import com.akshara.api.book.repository.BookEditionRepository;
import com.akshara.api.bookimport.dto.BookImportSource;
import com.akshara.api.bookimport.dto.ExternalBookResult;
import com.akshara.api.bookimport.dto.ExternalBookSearchPage;
import com.akshara.api.bookimport.dto.ExternalBookSearchQuery;
import com.akshara.api.bookimport.provider.BookMetadataProvider;
import com.akshara.api.common.exception.InvalidRequestException;
import com.akshara.api.bookimport.exception.ExternalCatalogueException;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
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
        if (source == BookImportSource.OPEN_LIBRARY) {
            result = enrichFromGoogleBooks(result, query, page, size);
        }
        List<ExternalBookResult> annotated = result.results().stream()
                .map(item -> item.withAlreadyImported(alreadyImported(item)))
                .toList();
        return new ExternalBookSearchPage(
                result.source(), annotated, result.page(), result.size(),
                result.totalResults()
        );
    }

    private ExternalBookSearchPage enrichFromGoogleBooks(
            ExternalBookSearchPage primary,
            String query,
            int page,
            int size
    ) {
        BookMetadataProvider fallback = providers.get(BookImportSource.GOOGLE_BOOKS);
        boolean needsEnrichment = primary.results().stream().anyMatch(item ->
                missing(item.description()) || missing(item.coverImageUrl())
                        || item.pageCount() == null || missing(item.publisher())
        );
        if (fallback == null || !needsEnrichment || primary.results().isEmpty()) return primary;

        try {
            List<ExternalBookResult> alternatives = fallback.search(
                    new ExternalBookSearchQuery(query, page, size)
            ).results();
            List<ExternalBookResult> enriched = primary.results().stream()
                    .map(item -> merge(item, findMatch(item, alternatives)))
                    .toList();
            return new ExternalBookSearchPage(
                    primary.source(), enriched, primary.page(), primary.size(), primary.totalResults()
            );
        } catch (ExternalCatalogueException ignored) {
            // Open Library results are still useful when the optional fallback is unavailable.
            return primary;
        }
    }

    private ExternalBookResult findMatch(
            ExternalBookResult primary,
            List<ExternalBookResult> alternatives
    ) {
        return alternatives.stream().filter(candidate ->
                same(primary.isbn13(), candidate.isbn13())
                        || same(primary.isbn10(), candidate.isbn10())
                        || normalize(primary.title()).equals(normalize(candidate.title()))
        ).findFirst().orElse(null);
    }

    private ExternalBookResult merge(ExternalBookResult primary, ExternalBookResult fallback) {
        if (fallback == null) return primary;
        return new ExternalBookResult(
                primary.source(), primary.sourceId(), primary.editionId(), primary.title(),
                choose(primary.subtitle(), fallback.subtitle()),
                choose(primary.description(), fallback.description()),
                primary.authors().isEmpty() ? fallback.authors() : primary.authors(),
                choose(primary.publisher(), fallback.publisher()),
                choose(primary.languageCode(), fallback.languageCode()),
                primary.pageCount() == null ? fallback.pageCount() : primary.pageCount(),
                primary.categories().isEmpty() ? fallback.categories() : primary.categories(),
                choose(primary.coverImageUrl(), fallback.coverImageUrl()),
                choose(primary.publicationDate(), fallback.publicationDate()),
                choose(primary.isbn10(), fallback.isbn10()),
                choose(primary.isbn13(), fallback.isbn13()),
                choose(primary.editionName(), fallback.editionName()),
                primary.alreadyImported()
        );
    }

    private String choose(String primary, String fallback) {
        return missing(primary) ? fallback : primary;
    }

    private boolean same(String left, String right) {
        return !missing(left) && !missing(right) && left.equalsIgnoreCase(right);
    }

    private boolean missing(String value) {
        return value == null || value.isBlank();
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private boolean alreadyImported(ExternalBookResult result) {
        return (result.isbn10() != null
                && editionRepository.existsByIsbn10(result.isbn10()))
                || (result.isbn13() != null
                && editionRepository.existsByIsbn13(result.isbn13()));
    }
}
