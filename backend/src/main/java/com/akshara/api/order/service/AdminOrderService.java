package com.akshara.api.order.service;

import com.akshara.api.book.entity.Inventory;
import com.akshara.api.book.repository.InventoryRepository;
import com.akshara.api.common.exception.InvalidRequestException;
import com.akshara.api.common.exception.ResourceNotFoundException;
import com.akshara.api.order.dto.AdminOrderDetailResponse;
import com.akshara.api.order.dto.AdminOrderSummaryResponse;
import com.akshara.api.order.entity.Order;
import com.akshara.api.order.entity.OrderItem;
import com.akshara.api.order.entity.OrderStatus;
import com.akshara.api.order.entity.Payment;
import com.akshara.api.order.repository.OrderItemRepository;
import com.akshara.api.order.repository.OrderRepository;
import com.akshara.api.order.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

@Service
public class AdminOrderService {

    private static final Map<OrderStatus, Set<OrderStatus>>
            ALLOWED_TRANSITIONS = createAllowedTransitions();

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final InventoryRepository inventoryRepository;
    private final OrderResponseMapper orderResponseMapper;

    public AdminOrderService(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            PaymentRepository paymentRepository,
            InventoryRepository inventoryRepository,
            OrderResponseMapper orderResponseMapper
    ) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.paymentRepository = paymentRepository;
        this.inventoryRepository = inventoryRepository;
        this.orderResponseMapper = orderResponseMapper;
    }

    @Transactional(readOnly = true)
    public List<AdminOrderSummaryResponse> getOrders() {
        return orderRepository
                .findAllByOrderByPlacedAtDesc()
                .stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminOrderDetailResponse getOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> orderNotFound(orderId));

        return loadDetailResponse(order);
    }

    @Transactional
    public AdminOrderDetailResponse updateStatus(
            Long orderId,
            OrderStatus requestedStatus
    ) {
        Order order = orderRepository
                .findByIdForUpdate(orderId)
                .orElseThrow(() -> orderNotFound(orderId));

        validateTransition(
                order.getStatus(),
                requestedStatus
        );

        List<OrderItem> orderItems = orderItemRepository
                .findAllByOrder_IdOrderByIdAsc(orderId);

        Payment payment = paymentRepository
                .findByOrder_Id(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment for order with ID "
                                + orderId
                                + " was not found"
                ));

        if (requestedStatus == OrderStatus.CANCELLED) {
            restoreInventory(orderItems);
            payment.cancel();
            paymentRepository.save(payment);
        }

        if (requestedStatus == OrderStatus.DELIVERED) {
            payment.markPaid();
            paymentRepository.save(payment);
        }

        order.setStatus(requestedStatus);
        orderRepository.save(order);

        return toDetailResponse(
                order,
                orderItems,
                payment
        );
    }

    private void validateTransition(
            OrderStatus currentStatus,
            OrderStatus requestedStatus
    ) {
        Set<OrderStatus> allowedStatuses =
                ALLOWED_TRANSITIONS.getOrDefault(
                        currentStatus,
                        Set.of()
                );

        if (!allowedStatuses.contains(requestedStatus)) {
            throw new InvalidRequestException(
                    "Order status cannot change from "
                            + currentStatus
                            + " to "
                            + requestedStatus
            );
        }
    }

    private void restoreInventory(
            List<OrderItem> orderItems
    ) {
        Map<Long, Integer> quantityByEditionId =
                new TreeMap<>();

        for (OrderItem orderItem : orderItems) {
            Long editionId =
                    orderItem.getBookEdition().getId();

            quantityByEditionId.merge(
                    editionId,
                    orderItem.getQuantity(),
                    Integer::sum
            );
        }

        List<Inventory> restoredInventories =
                new ArrayList<>();

        for (Map.Entry<Long, Integer> entry
                : quantityByEditionId.entrySet()) {

            Inventory inventory = inventoryRepository
                    .findByBookEditionIdForUpdate(
                            entry.getKey()
                    )
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Inventory for book edition with ID "
                                            + entry.getKey()
                                            + " was not found"
                            )
                    );

            inventory.increaseStock(entry.getValue());
            restoredInventories.add(inventory);
        }

        inventoryRepository.saveAll(restoredInventories);
    }

    private AdminOrderDetailResponse loadDetailResponse(
            Order order
    ) {
        List<OrderItem> orderItems = orderItemRepository
                .findAllByOrder_IdOrderByIdAsc(
                        order.getId()
                );

        Payment payment = paymentRepository
                .findByOrder_Id(order.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment for order with ID "
                                + order.getId()
                                + " was not found"
                ));

        return toDetailResponse(
                order,
                orderItems,
                payment
        );
    }

    private AdminOrderDetailResponse toDetailResponse(
            Order order,
            List<OrderItem> orderItems,
            Payment payment
    ) {
        return new AdminOrderDetailResponse(
                order.getUser().getId(),
                order.getUser().getFullName(),
                order.getUser().getEmail(),
                orderResponseMapper.toCheckoutResponse(
                        order,
                        orderItems,
                        payment
                )
        );
    }

    private AdminOrderSummaryResponse toSummaryResponse(
            Order order
    ) {
        return new AdminOrderSummaryResponse(
                order.getId(),
                order.getUser().getId(),
                order.getUser().getFullName(),
                order.getUser().getEmail(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getPlacedAt(),
                order.getUpdatedAt()
        );
    }

    private ResourceNotFoundException orderNotFound(
            Long orderId
    ) {
        return new ResourceNotFoundException(
                "Order with ID "
                        + orderId
                        + " was not found"
        );
    }

    private static Map<OrderStatus, Set<OrderStatus>>
    createAllowedTransitions() {

        Map<OrderStatus, Set<OrderStatus>> transitions =
                new EnumMap<>(OrderStatus.class);

        transitions.put(
                OrderStatus.PLACED,
                Set.of(
                        OrderStatus.CONFIRMED,
                        OrderStatus.CANCELLED
                )
        );

        transitions.put(
                OrderStatus.CONFIRMED,
                Set.of(
                        OrderStatus.PROCESSING,
                        OrderStatus.CANCELLED
                )
        );

        transitions.put(
                OrderStatus.PROCESSING,
                Set.of(
                        OrderStatus.SHIPPED,
                        OrderStatus.CANCELLED
                )
        );

        transitions.put(
                OrderStatus.SHIPPED,
                Set.of(OrderStatus.DELIVERED)
        );

        transitions.put(
                OrderStatus.DELIVERED,
                Set.of()
        );

        transitions.put(
                OrderStatus.CANCELLED,
                Set.of()
        );

        return Map.copyOf(transitions);
    }
}