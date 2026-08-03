package com.akshara.api.bookimport.controller;

import com.akshara.api.bookimport.dto.BookImportSource;
import com.akshara.api.bookimport.dto.CatalogueSeedResponse;
import com.akshara.api.bookimport.dto.ExternalBookSearchPage;
import com.akshara.api.bookimport.service.BookImportService;
import com.akshara.api.bookimport.service.CatalogueSeedService;
import com.akshara.api.bookimport.service.ExternalBookSearchService;
import com.akshara.api.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminBookImportController.class)
@Import(SecurityConfig.class)
class AdminBookImportControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private ExternalBookSearchService searchService;
    @MockitoBean private BookImportService importService;
    @MockitoBean private CatalogueSeedService seedService;
    @MockitoBean private JwtDecoder jwtDecoder;

    @Test
    void adminCanPopulateCuratedCatalogue() throws Exception {
        when(seedService.seed(50)).thenReturn(new CatalogueSeedResponse(
                50, 50, 3, 0, 50,
                Map.of("Classics", 7, "Science", 7),
                List.of("Pride and Prejudice", "A Brief History of Time")
        ));

        mockMvc.perform(post("/api/admin/book-import/seed")
                        .param("count", "50")
                        .with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(50))
                .andExpect(jsonPath("$.catalogueSizeAfter").value(50))
                .andExpect(jsonPath("$.genreCounts.Classics").value(7));

        verify(seedService).seed(50);
    }

    @Test
    void adminCanSearchWithBoundedPagination() throws Exception {
        when(searchService.search(
                "Atomic Habits", BookImportSource.OPEN_LIBRARY, 1, 10
        )).thenReturn(new ExternalBookSearchPage(
                BookImportSource.OPEN_LIBRARY, List.of(), 1, 10, 0
        ));

        mockMvc.perform(get("/api/admin/book-import/search")
                        .param("query", "Atomic Habits")
                        .with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("OPEN_LIBRARY"))
                .andExpect(jsonPath("$.results").isArray());

        verify(searchService).search(
                "Atomic Habits", BookImportSource.OPEN_LIBRARY, 1, 10
        );
    }

    @Test
    void readerCannotSearchExternalCatalogue() throws Exception {
        mockMvc.perform(get("/api/admin/book-import/search")
                        .param("query", "Atomic Habits")
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("ROLE_READER")
                        )))
                .andExpect(status().isForbidden());
        verifyNoInteractions(searchService, importService, seedService);
    }

    @Test
    void unauthenticatedClientCannotSearchExternalCatalogue() throws Exception {
        mockMvc.perform(get("/api/admin/book-import/search")
                        .param("query", "Atomic Habits"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(searchService, importService, seedService);
    }

    @Test
    void pageSizeAboveProviderLimitIsRejected() throws Exception {
        mockMvc.perform(get("/api/admin/book-import/search")
                        .param("query", "Atomic Habits")
                        .param("size", "21")
                        .with(adminJwt()))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(searchService, importService, seedService);
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor adminJwt() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }
}
