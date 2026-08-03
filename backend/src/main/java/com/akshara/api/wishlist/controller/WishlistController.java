package com.akshara.api.wishlist.controller;

import com.akshara.api.wishlist.dto.WishlistItemResponse;
import com.akshara.api.wishlist.service.WishlistService;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/wishlist")
@Validated
public class WishlistController {

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @GetMapping
    public List<WishlistItemResponse> getWishlist(@AuthenticationPrincipal Jwt jwt) {
        return wishlistService.getWishlist(jwt.getSubject());
    }

    @PostMapping("/{bookId}")
    public ResponseEntity<WishlistItemResponse> add(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable @Positive Long bookId
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(wishlistService.add(jwt.getSubject(), bookId));
    }

    @DeleteMapping("/{bookId}")
    public ResponseEntity<Void> remove(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable @Positive Long bookId
    ) {
        wishlistService.remove(jwt.getSubject(), bookId);
        return ResponseEntity.noContent().build();
    }
}
