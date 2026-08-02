package com.akshara.api.order.controller;

import com.akshara.api.order.dto.AdminOrderDetailResponse;
import com.akshara.api.order.dto.AdminOrderSummaryResponse;
import com.akshara.api.order.dto.UpdateOrderStatusRequest;
import com.akshara.api.order.service.AdminOrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    public AdminOrderController(
            AdminOrderService adminOrderService
    ) {
        this.adminOrderService = adminOrderService;
    }

    @GetMapping
    public ResponseEntity<List<AdminOrderSummaryResponse>>
    getOrders() {
        return ResponseEntity.ok(
                adminOrderService.getOrders()
        );
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<AdminOrderDetailResponse> getOrder(
            @PathVariable
            @Positive(message = "Order ID must be positive")
            Long orderId
    ) {
        return ResponseEntity.ok(
                adminOrderService.getOrder(orderId)
        );
    }

    @PatchMapping("/{orderId}/status")
    public ResponseEntity<AdminOrderDetailResponse> updateStatus(
            @PathVariable
            @Positive(message = "Order ID must be positive")
            Long orderId,

            @Valid
            @RequestBody
            UpdateOrderStatusRequest request
    ) {
        return ResponseEntity.ok(
                adminOrderService.updateStatus(
                        orderId,
                        request.status()
                )
        );
    }
}