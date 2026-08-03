package com.akshara.api.order.controller;

import com.akshara.api.common.exception.InvalidRequestException;
import com.akshara.api.common.exception.ResourceNotFoundException;
import com.akshara.api.config.SecurityConfig;
import com.akshara.api.order.dto.AdminOrderDetailResponse;
import com.akshara.api.order.entity.OrderStatus;
import com.akshara.api.order.service.AdminOrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request
        .SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request
        .MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request
        .MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result
        .MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result
        .MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result
        .MockMvcResultMatchers.status;

@WebMvcTest(AdminOrderController.class)
@Import(SecurityConfig.class)
class AdminOrderControllerTest {

    private static final Long ORDER_ID = 101L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminOrderService adminOrderService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void adminShouldListOrders() throws Exception {
        when(adminOrderService.getOrders())
                .thenReturn(List.of());

        mockMvc.perform(get("/api/admin/orders")
                        .with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(adminOrderService).getOrders();
    }

    @Test
    void adminShouldViewOrder() throws Exception {
        when(adminOrderService.getOrder(ORDER_ID))
                .thenReturn(detailResponse());

        mockMvc.perform(get(
                        "/api/admin/orders/{orderId}",
                        ORDER_ID
                ).with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(42))
                .andExpect(jsonPath("$.customerName")
                        .value("Ayush Thakur"))
                .andExpect(jsonPath("$.customerEmail")
                        .value("ayush@example.com"));

        verify(adminOrderService).getOrder(ORDER_ID);
    }

    @Test
    void adminShouldUpdateOrderStatus() throws Exception {
        when(adminOrderService.updateStatus(
                ORDER_ID,
                OrderStatus.CONFIRMED
        )).thenReturn(detailResponse());

        mockMvc.perform(patch(
                        "/api/admin/orders/{orderId}/status",
                        ORDER_ID
                )
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "CONFIRMED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(42));

        verify(adminOrderService).updateStatus(
                ORDER_ID,
                OrderStatus.CONFIRMED
        );
    }

    @Test
    void readerShouldReceiveForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/orders")
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority(
                                        "ROLE_READER"
                                )
                        )))
                .andExpect(status().isForbidden());

        verifyNoInteractions(adminOrderService);
    }

    @Test
    void unauthenticatedRequestShouldReceiveUnauthorized()
            throws Exception {

        mockMvc.perform(get("/api/admin/orders"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(adminOrderService);
    }

    @Test
    void missingStatusShouldReturnBadRequest() throws Exception {
        mockMvc.perform(patch(
                        "/api/admin/orders/{orderId}/status",
                        ORDER_ID
                )
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Request validation failed"))
                .andExpect(jsonPath("$.fieldErrors.status")
                        .value("Order status is required"));

        verifyNoInteractions(adminOrderService);
    }

    @Test
    void invalidStatusShouldReturnBadRequest() throws Exception {
        mockMvc.perform(patch(
                        "/api/admin/orders/{orderId}/status",
                        ORDER_ID
                )
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "PACKED"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Request body is missing or contains invalid JSON"
                ));

        verifyNoInteractions(adminOrderService);
    }

    @Test
    void nonPositiveOrderIdShouldReturnBadRequest()
            throws Exception {

        mockMvc.perform(get(
                        "/api/admin/orders/{orderId}",
                        0
                ).with(adminJwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Request validation failed"))
                .andExpect(jsonPath("$.fieldErrors.orderId")
                        .value("Order ID must be positive"));

        verifyNoInteractions(adminOrderService);
    }

    @Test
    void missingOrderShouldUseApiErrorResponse()
            throws Exception {

        when(adminOrderService.getOrder(ORDER_ID))
                .thenThrow(new ResourceNotFoundException(
                        "Order with ID 101 was not found"
                ));

        mockMvc.perform(get(
                        "/api/admin/orders/{orderId}",
                        ORDER_ID
                ).with(adminJwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error")
                        .value("Not Found"))
                .andExpect(jsonPath("$.message").value(
                        "Order with ID 101 was not found"
                ))
                .andExpect(jsonPath("$.path").value(
                        "/api/admin/orders/101"
                ));
    }

    @Test
    void invalidTransitionShouldUseApiErrorResponse()
            throws Exception {

        when(adminOrderService.updateStatus(
                ORDER_ID,
                OrderStatus.SHIPPED
        )).thenThrow(new InvalidRequestException(
                "Order status cannot change from PLACED to SHIPPED"
        ));

        mockMvc.perform(patch(
                        "/api/admin/orders/{orderId}/status",
                        ORDER_ID
                )
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "SHIPPED"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error")
                        .value("Bad Request"))
                .andExpect(jsonPath("$.message").value(
                        "Order status cannot change from "
                                + "PLACED to SHIPPED"
                ))
                .andExpect(jsonPath("$.path").value(
                        "/api/admin/orders/101/status"
                ));
    }

    private AdminOrderDetailResponse detailResponse() {
        return new AdminOrderDetailResponse(
                42L,
                "Ayush Thakur",
                "ayush@example.com",
                null
        );
    }

    private static org.springframework.test.web.servlet
            .request.RequestPostProcessor adminJwt() {

        return jwt().authorities(
                new SimpleGrantedAuthority("ROLE_ADMIN")
        );
    }
}