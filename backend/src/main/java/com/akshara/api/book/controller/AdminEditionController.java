package com.akshara.api.book.controller;

import com.akshara.api.book.dto.InventoryRequest;
import com.akshara.api.book.dto.InventoryResponse;
import com.akshara.api.book.dto.RestockRequest;
import com.akshara.api.book.service.BookService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/editions")
public class AdminEditionController {

    private final BookService bookService;

    public AdminEditionController(BookService bookService) {
        this.bookService = bookService;
    }

    @PutMapping("/{editionId}/inventory")
    public ResponseEntity<InventoryResponse> updateInventory(
            @PathVariable Long editionId,
            @Valid @RequestBody InventoryRequest request
    ) {
        return ResponseEntity.ok(
                bookService.updateInventory(editionId, request)
        );
    }

    @PostMapping("/{editionId}/inventory/restock")
    public ResponseEntity<InventoryResponse> restock(
            @PathVariable Long editionId,
            @Valid @RequestBody RestockRequest request
    ) {
        return ResponseEntity.ok(
                bookService.restockInventory(editionId, request.quantity())
        );
    }
}
