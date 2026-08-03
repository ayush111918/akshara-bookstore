package com.akshara.api.bookimport.service;

import com.akshara.api.book.repository.BookEditionRepository;
import com.akshara.api.bookimport.dto.BookImportSource;
import com.akshara.api.bookimport.dto.ExternalBookResult;
import com.akshara.api.bookimport.dto.ExternalBookSearchPage;
import com.akshara.api.bookimport.dto.ExternalBookSearchQuery;
import com.akshara.api.bookimport.provider.BookMetadataProvider;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExternalBookSearchServiceTest {

    @Test
    void enrichesMissingOpenLibraryFieldsWithoutChangingItsEditionIdentity() {
        BookMetadataProvider openLibrary = mock(BookMetadataProvider.class);
        BookMetadataProvider googleBooks = mock(BookMetadataProvider.class);
        BookEditionRepository editions = mock(BookEditionRepository.class);
        when(openLibrary.source()).thenReturn(BookImportSource.OPEN_LIBRARY);
        when(googleBooks.source()).thenReturn(BookImportSource.GOOGLE_BOOKS);
        when(openLibrary.search(any(ExternalBookSearchQuery.class))).thenReturn(new ExternalBookSearchPage(
                BookImportSource.OPEN_LIBRARY,
                List.of(result(BookImportSource.OPEN_LIBRARY, "/works/OL1W", null, null)),
                1, 10, 1
        ));
        when(googleBooks.search(any(ExternalBookSearchQuery.class))).thenReturn(new ExternalBookSearchPage(
                BookImportSource.GOOGLE_BOOKS,
                List.of(result(BookImportSource.GOOGLE_BOOKS, "volume-1", "A useful description", "https://cover.test/a.jpg")),
                1, 10, 1
        ));

        var page = new ExternalBookSearchService(List.of(openLibrary, googleBooks), editions)
                .search("Atomic Habits", BookImportSource.OPEN_LIBRARY, 1, 10);

        assertThat(page.results().get(0).source()).isEqualTo(BookImportSource.OPEN_LIBRARY);
        assertThat(page.results().get(0).sourceId()).isEqualTo("/works/OL1W");
        assertThat(page.results().get(0).description()).isEqualTo("A useful description");
        assertThat(page.results().get(0).coverImageUrl()).isEqualTo("https://cover.test/a.jpg");
    }

    private ExternalBookResult result(
            BookImportSource source,
            String sourceId,
            String description,
            String cover
    ) {
        return new ExternalBookResult(
                source, sourceId, sourceId, "Atomic Habits", null, description,
                List.of("James Clear"), null, "en", null, List.of(), cover,
                "2018-01-01", "0735211299", "9780735211292", null, false
        );
    }
}
