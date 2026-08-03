package com.akshara.api.book.controller;

import com.akshara.api.book.dto.InventoryRequest;
import com.akshara.api.book.dto.InventoryResponse;
import com.akshara.api.book.entity.AvailabilityStatus;
import com.akshara.api.book.service.BookService;
import com.akshara.api.bookimport.service.BookImportService;
import com.akshara.api.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({AdminBookController.class, AdminEditionController.class})
@Import(SecurityConfig.class)
class AdminCatalogueControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private BookService bookService;
    @MockitoBean private BookImportService bookImportService;
    @MockitoBean private JwtDecoder jwtDecoder;

    @Test
    void adminCanUpdateEditionInventory() throws Exception {
        InventoryResponse response = inventoryResponse(9, AvailabilityStatus.IN_STOCK);
        when(bookService.updateInventory(any(Long.class), any(InventoryRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/admin/editions/7/inventory")
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "price": 549.00,
                                  "stockQuantity": 9,
                                  "availabilityStatus": "IN_STOCK",
                                  "active": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockQuantity").value(9))
                .andExpect(jsonPath("$.price").value(549.00));

        verify(bookService).updateInventory(
                7L,
                new InventoryRequest(
                        new BigDecimal("549.00"), 9,
                        AvailabilityStatus.IN_STOCK, true
                )
        );
    }

    @Test
    void adminCanRestockAnEdition() throws Exception {
        when(bookService.restockInventory(7L, 12))
                .thenReturn(inventoryResponse(12, AvailabilityStatus.IN_STOCK));

        mockMvc.perform(post("/api/admin/editions/7/inventory/restock")
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":12}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockQuantity").value(12));

        verify(bookService).restockInventory(7L, 12);
    }

    @Test
    void invalidRestockQuantityIsRejectedBeforeServiceCall() throws Exception {
        mockMvc.perform(post("/api/admin/editions/7/inventory/restock")
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":0}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(bookService, bookImportService);
    }

    @Test
    void readerCannotChangeInventory() throws Exception {
        mockMvc.perform(post("/api/admin/editions/7/inventory/restock")
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("ROLE_READER")
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":3}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(bookService, bookImportService);
    }

    @Test
    void adminCanChangeFeaturedStatus() throws Exception {
        mockMvc.perform(patch("/api/admin/books/4/featured")
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"featured\":true}"))
                .andExpect(status().isOk());

        verify(bookService).updateFeatured(4L, true);
    }

    @Test
    void adminCanDeleteAnUnreferencedBook() throws Exception {
        mockMvc.perform(delete("/api/admin/books/4")
                        .with(adminJwt()))
                .andExpect(status().isNoContent());

        verify(bookService).delete(4L);
    }

    private InventoryResponse inventoryResponse(
            int stock,
            AvailabilityStatus status
    ) {
        return new InventoryResponse(
                3L, new BigDecimal("549.00"), stock,
                status, true, Instant.parse("2026-08-03T00:00:00Z")
        );
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor adminJwt() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }
}
