package com.akshara.api.order.service;

import com.akshara.api.auth.exception.InvalidAccessTokenException;
import com.akshara.api.common.exception.ResourceNotFoundException;
import com.akshara.api.book.entity.Book;
import com.akshara.api.book.entity.BookEdition;
import com.akshara.api.book.entity.BookFormat;
import com.akshara.api.order.dto.CheckoutResponse;
import com.akshara.api.order.dto.LibraryBookResponse;
import com.akshara.api.order.dto.OrderSummaryResponse;
import com.akshara.api.order.entity.Order;
import com.akshara.api.order.entity.OrderItem;
import com.akshara.api.order.entity.OrderStatus;
import com.akshara.api.order.entity.Payment;
import com.akshara.api.order.repository.OrderItemRepository;
import com.akshara.api.order.repository.OrderRepository;
import com.akshara.api.order.repository.PaymentRepository;
import com.akshara.api.user.entity.AppUser;
import com.akshara.api.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderQueryServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long ORDER_ID = 101L;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderResponseMapper orderResponseMapper;

    @Mock
    private AppUser user;

    @Mock
    private Order order;

    @Mock
    private OrderItem orderItem;

    @Mock
    private Payment payment;

    @Mock
    private BookEdition bookEdition;

    @Mock
    private Book book;

    @Mock
    private CheckoutResponse checkoutResponse;

    private OrderQueryService orderQueryService;

    @BeforeEach
    void setUp() {
        orderQueryService = new OrderQueryService(
                userRepository,
                orderRepository,
                orderItemRepository,
                paymentRepository,
                orderResponseMapper
        );
    }

    @Test
    void getOrdersShouldReturnAuthenticatedUsersOrders() {
        Instant placedAt =
                Instant.parse("2026-08-02T16:42:29Z");

        stubAuthenticatedUser();

        when(orderRepository
                .findAllByUser_IdOrderByPlacedAtDesc(USER_ID))
                .thenReturn(List.of(order));

        when(order.getId()).thenReturn(ORDER_ID);
        when(order.getStatus()).thenReturn(OrderStatus.PLACED);
        when(order.getTotalAmount())
                .thenReturn(new BigDecimal("598.00"));
        when(order.getPlacedAt()).thenReturn(placedAt);

        List<OrderSummaryResponse> response =
                orderQueryService.getOrders(
                        USER_ID.toString()
                );

        assertThat(response).containsExactly(
                new OrderSummaryResponse(
                        ORDER_ID,
                        OrderStatus.PLACED,
                        new BigDecimal("598.00"),
                        placedAt
                )
        );
    }

    @Test
    void getOrderShouldReturnOwnedOrderDetails() {
        stubAuthenticatedUser();

        when(orderRepository.findByIdAndUser_Id(
                ORDER_ID,
                USER_ID
        )).thenReturn(Optional.of(order));

        when(orderItemRepository
                .findAllByOrder_IdOrderByIdAsc(ORDER_ID))
                .thenReturn(List.of(orderItem));

        when(paymentRepository.findByOrder_Id(ORDER_ID))
                .thenReturn(Optional.of(payment));

        when(orderResponseMapper.toCheckoutResponse(
                order,
                List.of(orderItem),
                payment
        )).thenReturn(checkoutResponse);

        CheckoutResponse response =
                orderQueryService.getOrder(
                        USER_ID.toString(),
                        ORDER_ID
                );

        assertThat(response).isSameAs(checkoutResponse);

        verify(orderRepository)
                .findByIdAndUser_Id(ORDER_ID, USER_ID);

        verify(orderResponseMapper)
                .toCheckoutResponse(
                        order,
                        List.of(orderItem),
                        payment
                );
    }

    @Test
    void getOrderShouldHideAnotherUsersOrder() {
        stubAuthenticatedUser();

        when(orderRepository.findByIdAndUser_Id(
                ORDER_ID,
                USER_ID
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                orderQueryService.getOrder(
                        USER_ID.toString(),
                        ORDER_ID
                ))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(
                        "Order with ID 101 was not found"
                );

        verifyNoInteractions(
                orderItemRepository,
                paymentRepository,
                orderResponseMapper
        );
    }

    @Test
    void getOrdersShouldRejectInvalidJwtSubject() {
        assertThatThrownBy(() ->
                orderQueryService.getOrders(
                        "not-a-user-id"
                ))
                .isInstanceOf(
                        InvalidAccessTokenException.class
                );

        verifyNoInteractions(
                userRepository,
                orderRepository,
                orderItemRepository,
                paymentRepository,
                orderResponseMapper
        );
    }

    @Test
    void getLibraryShouldReturnBooksFromNonCancelledOrders() {
        Instant placedAt = Instant.parse("2026-08-03T08:18:00Z");
        stubAuthenticatedUser();

        when(orderItemRepository
                .findAllByOrder_User_IdAndOrder_StatusNotOrderByOrder_PlacedAtDescIdDesc(
                        USER_ID,
                        OrderStatus.CANCELLED
                )).thenReturn(List.of(orderItem));
        when(orderItem.getId()).thenReturn(501L);
        when(orderItem.getOrder()).thenReturn(order);
        when(orderItem.getBookEdition()).thenReturn(bookEdition);
        when(orderItem.getBookTitle()).thenReturn("The Discovery of India");
        when(orderItem.getCoverImageUrl()).thenReturn("https://example.test/cover.jpg");
        when(orderItem.getBookFormat()).thenReturn(BookFormat.PAPERBACK);
        when(orderItem.getIsbn13()).thenReturn("9780143031031");
        when(orderItem.getPublisherName()).thenReturn("Penguin");
        when(orderItem.getQuantity()).thenReturn(1);
        when(order.getId()).thenReturn(ORDER_ID);
        when(order.getStatus()).thenReturn(OrderStatus.SHIPPED);
        when(order.getPlacedAt()).thenReturn(placedAt);
        when(bookEdition.getId()).thenReturn(201L);
        when(bookEdition.getBook()).thenReturn(book);
        when(book.getId()).thenReturn(301L);

        assertThat(orderQueryService.getLibrary(USER_ID.toString()))
                .containsExactly(new LibraryBookResponse(
                        501L,
                        ORDER_ID,
                        301L,
                        201L,
                        "The Discovery of India",
                        "https://example.test/cover.jpg",
                        BookFormat.PAPERBACK,
                        null,
                        "9780143031031",
                        "Penguin",
                        1,
                        OrderStatus.SHIPPED,
                        placedAt
                ));
    }

    private void stubAuthenticatedUser() {
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));

        when(user.isEnabled()).thenReturn(true);
        when(user.getId()).thenReturn(USER_ID);
    }
}
