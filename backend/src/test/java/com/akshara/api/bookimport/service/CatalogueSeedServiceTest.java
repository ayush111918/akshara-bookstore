package com.akshara.api.bookimport.service;

import com.akshara.api.book.repository.BookRepository;
import com.akshara.api.bookimport.dto.BookImportRequest;
import com.akshara.api.bookimport.dto.BookImportSource;
import com.akshara.api.bookimport.dto.CatalogueSeedResponse;
import com.akshara.api.bookimport.dto.ExternalBookResult;
import com.akshara.api.bookimport.dto.ExternalBookSearchPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogueSeedServiceTest {

    @Mock private ExternalBookSearchService searchService;
    @Mock private BookImportService importService;
    @Mock private BookRepository bookRepository;

    @Test
    void importsFiftyBooksAcrossEveryCuratedGenre() {
        AtomicInteger sequence = new AtomicInteger();
        when(bookRepository.count()).thenReturn(1L, 51L);
        when(searchService.search(
                anyString(), eq(BookImportSource.OPEN_LIBRARY), anyInt(), eq(20)
        )).thenAnswer(invocation -> results(
                invocation.getArgument(0), sequence.getAndAdd(20)
        ));

        CatalogueSeedResponse response = new CatalogueSeedService(
                searchService, importService, bookRepository
        ).seed(50);

        assertThat(response.imported()).isEqualTo(50);
        assertThat(response.catalogueSizeBefore()).isEqualTo(1);
        assertThat(response.catalogueSizeAfter()).isEqualTo(51);
        assertThat(response.genreCounts()).hasSize(8);
        assertThat(response.genreCounts().values()).allMatch(count -> count > 0);

        ArgumentCaptor<BookImportRequest> requests =
                ArgumentCaptor.forClass(BookImportRequest.class);
        verify(importService, times(50)).importBook(requests.capture());
        assertThat(requests.getAllValues())
                .allMatch(request -> request.categories() != null
                        && !request.categories().isEmpty())
                .allMatch(request -> request.sku().startsWith("AKS-SEED-"));
    }

    private ExternalBookSearchPage results(String query, int start) {
        List<ExternalBookResult> results = new ArrayList<>();
        for (int index = 0; index < 20; index++) {
            String isbn = isbn13(start + index);
            results.add(new ExternalBookResult(
                    BookImportSource.OPEN_LIBRARY,
                    "/works/OL" + (start + index) + "W",
                    "/books/OL" + (start + index) + "M",
                    "Curated book " + (start + index),
                    null,
                    "A catalogue seed test book.",
                    List.of("Test Author"),
                    "Test Publisher",
                    "eng",
                    240,
                    List.of(),
                    "https://covers.openlibrary.org/test.jpg",
                    "2020-01-01",
                    null,
                    isbn,
                    query,
                    false
            ));
        }
        return new ExternalBookSearchPage(
                BookImportSource.OPEN_LIBRARY, results, 1, 20, 200
        );
    }

    private String isbn13(int value) {
        String firstTwelve = "978" + String.format("%09d", value);
        int sum = 0;
        for (int index = 0; index < firstTwelve.length(); index++) {
            int digit = firstTwelve.charAt(index) - '0';
            sum += digit * (index % 2 == 0 ? 1 : 3);
        }
        return firstTwelve + ((10 - sum % 10) % 10);
    }
}
