package com.akshara.api.reading.controller;

import com.akshara.api.config.SecurityConfig;
import com.akshara.api.reading.dto.ReadingDashboardResponse;
import com.akshara.api.reading.service.ReadingJourneyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReadingJourneyController.class)
@Import(SecurityConfig.class)
class ReadingJourneyControllerTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean ReadingJourneyService service;
    @MockitoBean JwtDecoder jwtDecoder;

    @Test
    void readerCanOpenOnlyTheirDashboard() throws Exception {
        when(service.getDashboard("42")).thenReturn(new ReadingDashboardResponse(null, null, java.util.List.of()));

        mockMvc.perform(get("/api/reading-journey").with(readerJwt()))
                .andExpect(status().isOk());

        verify(service).getDashboard("42");
    }

    @Test
    void unauthenticatedVisitorCannotOpenDashboard() throws Exception {
        mockMvc.perform(get("/api/reading-journey"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test
    void adminCannotUseReaderJourneyEndpoints() throws Exception {
        mockMvc.perform(get("/api/reading-journey").with(jwt()
                        .jwt(token -> token.subject("1"))
                        .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    void invalidEntryIdIsRejectedBeforeServiceInvocation() throws Exception {
        mockMvc.perform(put("/api/reading-journey/0/progress")
                        .with(readerJwt())
                        .contentType("application/json")
                        .content("{\"status\":\"READING\",\"currentPage\":10}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor readerJwt() {
        return jwt().jwt(token -> token.subject("42"))
                .authorities(new SimpleGrantedAuthority("ROLE_READER"));
    }
}
