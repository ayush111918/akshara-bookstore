package com.akshara.api.cart.controller;

import com.akshara.api.cart.dto.AddCartItemRequest;
import com.akshara.api.cart.dto.CartResponse;
import com.akshara.api.cart.dto.UpdateCartItemRequest;
import com.akshara.api.cart.service.CartService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
@Validated
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public CartResponse getCart(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return cartService.getCart(jwt.getSubject());
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItem(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AddCartItemRequest request
    ) {
        CartResponse response = cartService.addItem(
                jwt.getSubject(),
                request
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PatchMapping("/items/{itemId}")
    public CartResponse updateItem(
            @AuthenticationPrincipal Jwt jwt,

            @PathVariable
            @Positive(message = "Cart item ID must be positive")
            Long itemId,

            @Valid @RequestBody UpdateCartItemRequest request
    ) {
        return cartService.updateItem(
                jwt.getSubject(),
                itemId,
                request
        );
    }

    @DeleteMapping("/items/{itemId}")
    public CartResponse removeItem(
            @AuthenticationPrincipal Jwt jwt,

            @PathVariable
            @Positive(message = "Cart item ID must be positive")
            Long itemId
    ) {
        return cartService.removeItem(
                jwt.getSubject(),
                itemId
        );
    }

    @DeleteMapping("/items")
    public CartResponse clearCart(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return cartService.clearCart(jwt.getSubject());
    }
}