package com.akshara.api.bookimport.service;

import com.akshara.api.book.entity.AvailabilityStatus;
import com.akshara.api.book.entity.BookFormat;
import com.akshara.api.book.repository.BookRepository;
import com.akshara.api.bookimport.dto.BookImportRequest;
import com.akshara.api.bookimport.dto.BookImportSource;
import com.akshara.api.bookimport.dto.CatalogueSeedResponse;
import com.akshara.api.bookimport.dto.ExternalBookResult;
import com.akshara.api.common.exception.DuplicateResourceException;
import com.akshara.api.common.exception.InvalidRequestException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class CatalogueSeedService {

    private static final List<CuratedSearch> CURATED_SEARCHES = List.of(
            new CuratedSearch("Classics", "subject:classic literature language:eng"),
            new CuratedSearch("Science", "subject:science language:eng"),
            new CuratedSearch("History", "subject:history language:eng"),
            new CuratedSearch("Philosophy", "subject:philosophy language:eng"),
            new CuratedSearch("Business", "subject:business language:eng"),
            new CuratedSearch("Biography", "subject:biography language:eng"),
            new CuratedSearch("Indian Literature", "subject:india literature language:eng"),
            new CuratedSearch("Children's", "subject:children language:eng")
    );

    private final ExternalBookSearchService searchService;
    private final BookImportService importService;
    private final BookRepository bookRepository;

    public CatalogueSeedService(
            ExternalBookSearchService searchService,
            BookImportService importService,
            BookRepository bookRepository
    ) {
        this.searchService = searchService;
        this.importService = importService;
        this.bookRepository = bookRepository;
    }

    public CatalogueSeedResponse seed(int requested) {
        long before = bookRepository.count();
        int imported = 0;
        int skipped = 0;
        Set<String> attemptedWorks = new HashSet<>();
        List<String> importedTitles = new ArrayList<>();
        int perGenreTarget = (int) Math.ceil(
                requested / (double) CURATED_SEARCHES.size()
        );
        Map<String, Integer> importedByGenre = new LinkedHashMap<>();

        for (int page = 1; page <= 5 && imported < requested; page++) {
            for (CuratedSearch search : CURATED_SEARCHES) {
                if (imported >= requested) break;
                if (importedByGenre.getOrDefault(search.genre(), 0) >= perGenreTarget) {
                    continue;
                }
                List<ExternalBookResult> results = searchService.search(
                        search.query(), BookImportSource.OPEN_LIBRARY, page, 20
                ).results();

                for (ExternalBookResult result : results) {
                    if (imported >= requested
                            || importedByGenre.getOrDefault(search.genre(), 0)
                            >= perGenreTarget) break;
                    String identity = identity(result);
                    if (identity == null
                            || !attemptedWorks.add(identity)
                            || result.alreadyImported()
                            || !hasIsbn(result)) {
                        skipped++;
                        continue;
                    }
                    try {
                        importService.importBook(toRequest(result, imported, search.genre()));
                        importedTitles.add(result.title());
                        imported++;
                        importedByGenre.merge(search.genre(), 1, Integer::sum);
                    } catch (DuplicateResourceException | InvalidRequestException exception) {
                        skipped++;
                    } catch (RuntimeException exception) {
                        skipped++;
                    }
                }
            }
        }

        return new CatalogueSeedResponse(
                requested, imported, skipped, before,
                bookRepository.count(), Map.copyOf(importedByGenre),
                List.copyOf(importedTitles)
        );
    }

    private BookImportRequest toRequest(
            ExternalBookResult result,
            int index,
            String genre
    ) {
        String isbn = cleanIsbn(result.isbn13() != null
                ? result.isbn13() : result.isbn10());
        List<String> authors = sanitizedList(result.authors(), 150, 4);
        if (authors.isEmpty()) authors = List.of("Unknown Author");
        List<String> categories = categories(genre, result.categories());
        int priceBand = Math.floorMod(isbn.hashCode(), 6);
        BigDecimal price = BigDecimal.valueOf(249L + priceBand * 50L)
                .setScale(2);
        int stock = 8 + Math.floorMod(isbn.hashCode(), 18);

        return new BookImportRequest(
                BookImportSource.OPEN_LIBRARY,
                limited(result.sourceId(), 255),
                limited(result.editionId(), 255),
                requiredText(result.title(), "Untitled", 255),
                limited(result.subtitle(), 255),
                limited(result.description(), 20000),
                authors,
                limited(result.publisher(), 150),
                language(result.languageCode()),
                result.pageCount() != null && result.pageCount() > 0
                        ? result.pageCount() : null,
                categories,
                limited(result.coverImageUrl(), 1000),
                date(result.publicationDate()),
                limited(result.editionName(), 100),
                cleanOptionalIsbn(result.isbn10()),
                cleanOptionalIsbn(result.isbn13()),
                BookFormat.PAPERBACK,
                price,
                stock,
                AvailabilityStatus.IN_STOCK,
                "AKS-SEED-" + isbn,
                true,
                index < 12
        );
    }

    private List<String> categories(String genre, List<String> sourceCategories) {
        Set<String> categories = new LinkedHashSet<>();
        categories.add(genre);
        categories.addAll(sanitizedList(sourceCategories, 100, 3));
        return List.copyOf(categories);
    }

    private String identity(ExternalBookResult result) {
        if (result.sourceId() != null && !result.sourceId().isBlank()) {
            return result.sourceId().trim();
        }
        String isbn = result.isbn13() != null ? result.isbn13() : result.isbn10();
        return isbn == null || isbn.isBlank() ? null : cleanIsbn(isbn);
    }

    private boolean hasIsbn(ExternalBookResult result) {
        return (result.isbn13() != null && !result.isbn13().isBlank())
                || (result.isbn10() != null && !result.isbn10().isBlank());
    }

    private List<String> sanitizedList(List<String> values, int maxLength, int maxItems) {
        if (values == null) return List.of();
        Set<String> sanitized = new LinkedHashSet<>();
        for (String value : values) {
            String text = limited(value, maxLength);
            if (text != null && !text.isBlank()) sanitized.add(text);
            if (sanitized.size() == maxItems) break;
        }
        return List.copyOf(sanitized);
    }

    private String requiredText(String value, String fallback, int maxLength) {
        String text = value == null || value.isBlank() ? fallback : value.trim();
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }

    private String limited(String value, int maxLength) {
        if (value == null || value.isBlank()) return null;
        String text = value.trim();
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }

    private String language(String value) {
        if (value == null) return "en";
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.matches("^[a-z]{2,3}$") ? normalized : "en";
    }

    private LocalDate date(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private String cleanOptionalIsbn(String value) {
        return value == null || value.isBlank() ? null : cleanIsbn(value);
    }

    private String cleanIsbn(String value) {
        return value.replaceAll("[-\\s]", "").toUpperCase(Locale.ROOT);
    }

    private record CuratedSearch(String genre, String query) {
    }
}
