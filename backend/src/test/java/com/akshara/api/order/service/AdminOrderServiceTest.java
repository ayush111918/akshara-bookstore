package com.akshara.api.order.service;

import com.akshara.api.book.entity.BookEdition;
import com.akshara.api.book.entity.Inventory;
import com.akshara.api.book.repository.InventoryRepository;
import com.akshara.api.common.exception.InvalidRequestException;
import com.akshara.api.common.exception.ResourceNotFoundException;
import com.akshara.api.order.dto.AdminOrderDetailResponse;
import com.akshara.api.order.dto.CheckoutResponse;
import com.akshara.api.order.entity.Order;
import com.akshara.api.order.entity.OrderItem;
import com.akshara.api.order.entity.OrderStatus;
import com.akshara.api.order.entity.Payment;
import com.akshara.api.order.repository.OrderItemRepository;
import com.akshara.api.order.repository.OrderRepository;
import com.akshara.api.order.repository.PaymentRepository;
import com.akshara.api.user.entity.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminOrderServiceTest {

    private static final Long ORDER_ID = 101L;
    private static final Long USER_ID = 1L;
    private static final Long EDITION_ID = 30L;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private OrderResponseMapper orderResponseMapper;

    @Mock
    private Order order;

    @Mock
    private Payment payment;

    @Mock
    private AppUser user;

    @Mock
    private OrderItem firstOrderItem;

    @Mock
    private OrderItem secondOrderItem;

    @Mock
    private BookEdition edition;

    @Mock
    private Inventory inventory;

    @Mock
    private CheckoutResponse checkoutResponse;

    private AdminOrderService adminOrderService;

    @BeforeEach
    void setUp() {
        adminOrderService = new AdminOrderService(
                orderRepository,
                orderItemRepository,
                paymentRepository,
                inventoryRepository,
                orderResponseMapper
        );
    }

    @Test
    void updateStatusShouldAllowNextLifecycleStatus() {
        List<OrderItem> orderItems = List.of();

        when(orderRepository.findByIdForUpdate(ORDER_ID))
                .thenReturn(Optional.of(order));

        when(order.getStatus())
                .thenReturn(OrderStatus.PLACED);

        when(orderItemRepository
                .findAllByOrder_IdOrderByIdAsc(ORDER_ID))
                .thenReturn(orderItems);

        when(paymentRepository.findByOrder_Id(ORDER_ID))
                .thenReturn(Optional.of(payment));

        stubDetailResponse(orderItems);

        AdminOrderDetailResponse response =
                adminOrderService.updateStatus(
                        ORDER_ID,
                        OrderStatus.CONFIRMED
                );

        assertThat(response.userId()).isEqualTo(USER_ID);
        assertThat(response.customerName())
                .isEqualTo("Ayush Thakur");
        assertThat(response.customerEmail())
                .isEqualTo("ayush@example.com");
        assertThat(response.order())
                .isSameAs(checkoutResponse);

        verify(order).setStatus(OrderStatus.CONFIRMED);
        verify(orderRepository).save(order);

        verify(payment, never()).cancel();
        verify(payment, never()).markPaid();
        verify(paymentRepository, never()).save(payment);

        verifyNoInteractions(inventoryRepository);
    }

    @Test
    void updateStatusShouldRejectInvalidJump() {
        when(orderRepository.findByIdForUpdate(ORDER_ID))
                .thenReturn(Optional.of(order));

        when(order.getStatus())
                .thenReturn(OrderStatus.PLACED);

        assertThatThrownBy(() ->
                adminOrderService.updateStatus(
                        ORDER_ID,
                        OrderStatus.SHIPPED
                ))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage(
                        "Order status cannot change from "
                                + "PLACED to SHIPPED"
                );

        verify(order, never())
                .setStatus(OrderStatus.SHIPPED);

        verify(orderRepository, never()).save(order);

        verifyNoInteractions(
                orderItemRepository,
                paymentRepository,
                inventoryRepository,
                orderResponseMapper
        );
    }

    @Test
    void cancelledOrderShouldBeTerminalAndPreventDuplicateRestoration() {
        when(orderRepository.findByIdForUpdate(ORDER_ID))
                .thenReturn(Optional.of(order));

        when(order.getStatus())
                .thenReturn(OrderStatus.CANCELLED);

        assertThatThrownBy(() ->
                adminOrderService.updateStatus(
                        ORDER_ID,
                        OrderStatus.CANCELLED
                ))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage(
                        "Order status cannot change from "
                                + "CANCELLED to CANCELLED"
                );

        verifyNoInteractions(
                orderItemRepository,
                paymentRepository,
                inventoryRepository,
                orderResponseMapper
        );

        verify(orderRepository, never()).save(order);
    }

    @Test
    void deliveredOrderShouldBeTerminal() {
        when(orderRepository.findByIdForUpdate(ORDER_ID))
                .thenReturn(Optional.of(order));

        when(order.getStatus())
                .thenReturn(OrderStatus.DELIVERED);

        assertThatThrownBy(() ->
                adminOrderService.updateStatus(
                        ORDER_ID,
                        OrderStatus.CANCELLED
                ))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage(
                        "Order status cannot change from "
                                + "DELIVERED to CANCELLED"
                );

        verifyNoInteractions(
                orderItemRepository,
                paymentRepository,
                inventoryRepository,
                orderResponseMapper
        );

        verify(orderRepository, never()).save(order);
    }

    @Test
    void cancellationShouldRestoreStockAndCancelPayment() {
        List<OrderItem> orderItems = List.of(
                firstOrderItem,
                secondOrderItem
        );

        when(orderRepository.findByIdForUpdate(ORDER_ID))
                .thenReturn(Optional.of(order));

        when(order.getStatus())
                .thenReturn(OrderStatus.PROCESSING);

        when(orderItemRepository
                .findAllByOrder_IdOrderByIdAsc(ORDER_ID))
                .thenReturn(orderItems);

        when(paymentRepository.findByOrder_Id(ORDER_ID))
                .thenReturn(Optional.of(payment));

        when(firstOrderItem.getBookEdition())
                .thenReturn(edition);

        when(secondOrderItem.getBookEdition())
                .thenReturn(edition);

        when(firstOrderItem.getQuantity()).thenReturn(2);
        when(secondOrderItem.getQuantity()).thenReturn(1);

        when(edition.getId()).thenReturn(EDITION_ID);

        when(inventoryRepository
                .findByBookEditionIdForUpdate(EDITION_ID))
                .thenReturn(Optional.of(inventory));

        stubDetailResponse(orderItems);

        AdminOrderDetailResponse response =
                adminOrderService.updateStatus(
                        ORDER_ID,
                        OrderStatus.CANCELLED
                );

        assertThat(response.order())
                .isSameAs(checkoutResponse);

        verify(inventory).increaseStock(3);
        verify(inventoryRepository)
                .saveAll(List.of(inventory));

        verify(payment).cancel();
        verify(payment, never()).markPaid();
        verify(paymentRepository).save(payment);

        verify(order).setStatus(OrderStatus.CANCELLED);
        verify(orderRepository).save(order);
    }

    @Test
    void deliveredStatusShouldMarkPaymentPaid() {
        List<OrderItem> orderItems = List.of();

        when(orderRepository.findByIdForUpdate(ORDER_ID))
                .thenReturn(Optional.of(order));

        when(order.getStatus())
                .thenReturn(OrderStatus.SHIPPED);

        when(orderItemRepository
                .findAllByOrder_IdOrderByIdAsc(ORDER_ID))
                .thenReturn(orderItems);

        when(paymentRepository.findByOrder_Id(ORDER_ID))
                .thenReturn(Optional.of(payment));

        stubDetailResponse(orderItems);

        adminOrderService.updateStatus(
                ORDER_ID,
                OrderStatus.DELIVERED
        );

        verify(payment).markPaid();
        verify(payment, never()).cancel();
        verify(paymentRepository).save(payment);

        verify(order).setStatus(OrderStatus.DELIVERED);
        verify(orderRepository).save(order);

        verifyNoInteractions(inventoryRepository);
    }

    @Test
    void updateStatusShouldRejectMissingOrder() {
        when(orderRepository.findByIdForUpdate(ORDER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                adminOrderService.updateStatus(
                        ORDER_ID,
                        OrderStatus.CONFIRMED
                ))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(
                        "Order with ID 101 was not found"
                );

        verifyNoInteractions(
                orderItemRepository,
                paymentRepository,
                inventoryRepository,
                orderResponseMapper
        );
    }

    private void stubDetailResponse(
            List<OrderItem> orderItems
    ) {
        when(order.getUser()).thenReturn(user);

        when(user.getId()).thenReturn(USER_ID);
        when(user.getFullName())
                .thenReturn("Ayush Thakur");
        when(user.getEmail())
                .thenReturn("ayush@example.com");

        when(orderResponseMapper.toCheckoutResponse(
                order,
                orderItems,
                payment
        )).thenReturn(checkoutResponse);
    }
}