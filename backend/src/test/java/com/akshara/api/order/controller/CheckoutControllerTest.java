package com.akshara.api.order.controller;

import com.akshara.api.order.dto.CheckoutRequest;
import com.akshara.api.order.dto.CheckoutResponse;
import com.akshara.api.order.dto.ShippingAddressRequest;
import com.akshara.api.order.service.CheckoutService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CheckoutControllerTest {

    @Test
    void checkoutShouldUseJwtSubjectAndReturnCreatedResponse() {
        CheckoutService checkoutService =
                mock(CheckoutService.class);

        Jwt jwt = mock(Jwt.class);

        CheckoutController controller =
                new CheckoutController(checkoutService);

        CheckoutRequest request = new CheckoutRequest(
                new ShippingAddressRequest(
                        "Ayush Thakur",
                        "9876543210",
                        "123 Main Road",
                        null,
                        "New Delhi",
                        "Delhi",
                        "110001",
                        "India"
                )
        );

        CheckoutResponse expectedResponse =
                new CheckoutResponse(
                        101L,
                        null,
                        null,
                        null,
                        List.of(),
                        0,
                        new BigDecimal("0.00"),
                        new BigDecimal("0.00"),
                        new BigDecimal("0.00"),
                        null,
                        Instant.now()
                );

        when(jwt.getSubject()).thenReturn("42");

        when(checkoutService.checkout("42", request))
                .thenReturn(expectedResponse);

        ResponseEntity<CheckoutResponse> actualResponse =
                controller.checkout(jwt, request);

        assertEquals(
                HttpStatus.CREATED,
                actualResponse.getStatusCode()
        );

        assertSame(
                expectedResponse,
                actualResponse.getBody()
        );

        verify(checkoutService).checkout("42", request);
    }
}