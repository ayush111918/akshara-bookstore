package com.akshara.api.order.service;

import com.akshara.api.book.entity.AvailabilityStatus;
import com.akshara.api.book.entity.Book;
import com.akshara.api.book.entity.BookEdition;
import com.akshara.api.book.entity.BookFormat;
import com.akshara.api.book.entity.Inventory;
import com.akshara.api.book.repository.InventoryRepository;
import com.akshara.api.cart.entity.Cart;
import com.akshara.api.cart.entity.CartItem;
import com.akshara.api.cart.repository.CartItemRepository;
import com.akshara.api.cart.repository.CartRepository;
import com.akshara.api.common.exception.InvalidRequestException;
import com.akshara.api.order.dto.CheckoutRequest;
import com.akshara.api.order.dto.CheckoutResponse;
import com.akshara.api.order.dto.ShippingAddressRequest;
import com.akshara.api.order.entity.Order;
import com.akshara.api.order.entity.OrderItem;
import com.akshara.api.order.entity.OrderStatus;
import com.akshara.api.order.entity.Payment;
import com.akshara.api.order.entity.PaymentMethod;
import com.akshara.api.order.entity.PaymentStatus;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckoutServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long CART_ID = 10L;
    private static final Long EDITION_ID = 30L;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private InventoryRepository inventoryRepository;

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

    private CheckoutService checkoutService;
    private Cart cart;
    private BookEdition edition;
    private CheckoutRequest checkoutRequest;

    @BeforeEach
    void setUp() {
        checkoutService = new CheckoutService(
                cartRepository,
                cartItemRepository,
                userRepository,
                inventoryRepository,
                orderRepository,
                orderItemRepository,
                paymentRepository,
                orderResponseMapper
        );

        when(user.getId()).thenReturn(USER_ID);
        when(user.isEnabled()).thenReturn(true);

        cart = new Cart(user);
        ReflectionTestUtils.setField(cart, "id", CART_ID);

        Book book = new Book("Clean Code");

        edition = new BookEdition(
                book,
                BookFormat.PAPERBACK
        );
        edition.setEditionName("First Edition");
        edition.setIsbn13("9780132350884");

        ReflectionTestUtils.setField(
                edition,
                "id",
                EDITION_ID
        );

        checkoutRequest = new CheckoutRequest(
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
    }

    @Test
    void checkoutShouldCreateOrderReduceStockAndClearCart() {
        CartItem cartItem = new CartItem(
                cart,
                edition,
                2
        );

        Inventory inventory = new Inventory(
                edition,
                new BigDecimal("299.00"),
                5,
                AvailabilityStatus.IN_STOCK
        );

        CheckoutResponse expectedResponse =
                new CheckoutResponse(
                        101L,
                        OrderStatus.PLACED,
                        PaymentMethod.CASH_ON_DELIVERY,
                        PaymentStatus.PENDING,
                        List.of(),
                        2,
                        new BigDecimal("598.00"),
                        new BigDecimal("0.00"),
                        new BigDecimal("598.00"),
                        null,
                        Instant.now()
                );

        stubAuthenticatedCart();

        when(cartItemRepository
                .findAllByCart_IdOrderByCreatedAtAsc(CART_ID))
                .thenReturn(List.of(cartItem));

        when(inventoryRepository
                .findByBookEditionIdForUpdate(EDITION_ID))
                .thenReturn(Optional.of(inventory));

        AtomicReference<Order> savedOrder =
                new AtomicReference<>();

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> {
                    Order order = invocation.getArgument(0);
                    savedOrder.set(order);
                    return order;
                });

        AtomicReference<List<OrderItem>> savedOrderItems =
                new AtomicReference<>();

        when(orderItemRepository.saveAll(anyList()))
                .thenAnswer(invocation -> {
                    List<OrderItem> items =
                            invocation.getArgument(0);

                    savedOrderItems.set(items);
                    return items;
                });

        AtomicReference<Payment> savedPayment =
                new AtomicReference<>();

        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> {
                    Payment payment = invocation.getArgument(0);
                    savedPayment.set(payment);
                    return payment;
                });

        when(orderResponseMapper.toCheckoutResponse(
                any(Order.class),
                anyList(),
                any(Payment.class)
        )).thenReturn(expectedResponse);

        CheckoutResponse actualResponse =
                checkoutService.checkout(
                        USER_ID.toString(),
                        checkoutRequest
                );

        assertThat(actualResponse)
                .isSameAs(expectedResponse);

        assertThat(inventory.getStockQuantity())
                .isEqualTo(3);

        assertThat(inventory.getAvailabilityStatus())
                .isEqualTo(AvailabilityStatus.IN_STOCK);

        Order order = savedOrder.get();

        assertThat(order.getSubtotal())
                .isEqualByComparingTo("598.00");

        assertThat(order.getShippingFee())
                .isEqualByComparingTo("0.00");

        assertThat(order.getTotalAmount())
                .isEqualByComparingTo("598.00");

        assertThat(order.getShippingAddress().getRecipientName())
                .isEqualTo("Ayush Thakur");

        assertThat(order.getShippingAddress().getPostalCode())
                .isEqualTo("110001");

        assertThat(savedOrderItems.get())
                .hasSize(1);

        OrderItem orderItem =
                savedOrderItems.get().get(0);

        assertThat(orderItem.getBookTitle())
                .isEqualTo("Clean Code");

        assertThat(orderItem.getBookFormat())
                .isEqualTo(BookFormat.PAPERBACK);

        assertThat(orderItem.getUnitPrice())
                .isEqualByComparingTo("299.00");

        assertThat(orderItem.getQuantity())
                .isEqualTo(2);

        assertThat(orderItem.getSubtotal())
                .isEqualByComparingTo("598.00");

        Payment payment = savedPayment.get();

        assertThat(payment.getMethod())
                .isEqualTo(PaymentMethod.CASH_ON_DELIVERY);

        assertThat(payment.getStatus())
                .isEqualTo(PaymentStatus.PENDING);

        assertThat(payment.getAmount())
                .isEqualByComparingTo("598.00");

        verify(inventoryRepository)
                .findByBookEditionIdForUpdate(EDITION_ID);

        verify(cartItemRepository)
                .deleteAllByCart_Id(CART_ID);

        verify(cartRepository).save(cart);
    }

    @Test
    void checkoutShouldRejectMissingOrEmptyCart() {
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));

        when(cartRepository.findByUserIdForUpdate(USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                checkoutService.checkout(
                        USER_ID.toString(),
                        checkoutRequest
                ))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Cart is empty");

        verifyNoInteractions(
                inventoryRepository,
                orderRepository,
                orderItemRepository,
                paymentRepository,
                orderResponseMapper
        );
    }

    @Test
    void checkoutShouldRejectQuantityGreaterThanStock() {
        CartItem cartItem = new CartItem(
                cart,
                edition,
                2
        );

        Inventory inventory = new Inventory(
                edition,
                new BigDecimal("299.00"),
                1,
                AvailabilityStatus.IN_STOCK
        );

        stubAuthenticatedCart();

        when(cartItemRepository
                .findAllByCart_IdOrderByCreatedAtAsc(CART_ID))
                .thenReturn(List.of(cartItem));

        when(inventoryRepository
                .findByBookEditionIdForUpdate(EDITION_ID))
                .thenReturn(Optional.of(inventory));

        assertThatThrownBy(() ->
                checkoutService.checkout(
                        USER_ID.toString(),
                        checkoutRequest
                ))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Insufficient stock")
                .hasMessageContaining(
                        "Available quantity: 1"
                );

        assertThat(inventory.getStockQuantity())
                .isEqualTo(1);

        verify(orderRepository, never())
                .save(any(Order.class));

        verify(paymentRepository, never())
                .save(any(Payment.class));

        verify(cartItemRepository, never())
                .deleteAllByCart_Id(anyLong());
    }

    private void stubAuthenticatedCart() {
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));

        when(cartRepository.findByUserIdForUpdate(USER_ID))
                .thenReturn(Optional.of(cart));
    }
}
