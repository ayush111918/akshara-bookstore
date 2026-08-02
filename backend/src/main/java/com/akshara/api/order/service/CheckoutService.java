package com.akshara.api.order.service;

import com.akshara.api.auth.exception.InvalidAccessTokenException;
import com.akshara.api.book.entity.AvailabilityStatus;
import com.akshara.api.book.entity.Inventory;
import com.akshara.api.book.repository.InventoryRepository;
import com.akshara.api.cart.entity.Cart;
import com.akshara.api.cart.entity.CartItem;
import com.akshara.api.cart.repository.CartItemRepository;
import com.akshara.api.cart.repository.CartRepository;
import com.akshara.api.common.exception.InvalidRequestException;
import com.akshara.api.common.exception.ResourceNotFoundException;
import com.akshara.api.order.dto.CheckoutRequest;
import com.akshara.api.order.dto.CheckoutResponse;
import com.akshara.api.order.dto.ShippingAddressRequest;
import com.akshara.api.order.entity.Address;
import com.akshara.api.order.entity.Order;
import com.akshara.api.order.entity.OrderItem;
import com.akshara.api.order.entity.Payment;
import com.akshara.api.order.entity.PaymentMethod;
import com.akshara.api.order.repository.OrderItemRepository;
import com.akshara.api.order.repository.OrderRepository;
import com.akshara.api.order.repository.PaymentRepository;
import com.akshara.api.user.entity.AppUser;
import com.akshara.api.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CheckoutService {

    private static final BigDecimal SHIPPING_FEE =
            new BigDecimal("0.00");

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final InventoryRepository inventoryRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final OrderResponseMapper orderResponseMapper;

    public CheckoutService(
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            UserRepository userRepository,
            InventoryRepository inventoryRepository,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            PaymentRepository paymentRepository,
            OrderResponseMapper orderResponseMapper
    ) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.userRepository = userRepository;
        this.inventoryRepository = inventoryRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.paymentRepository = paymentRepository;
        this.orderResponseMapper = orderResponseMapper;
    }

    @Transactional
    public CheckoutResponse checkout(
            String subject,
            CheckoutRequest request
    ) {
        AppUser user = getAuthenticatedUser(subject);

        Cart cart = cartRepository
                .findByUserIdForUpdate(user.getId())
                .orElseThrow(() -> new InvalidRequestException(
                        "Cart is empty"
                ));

        List<CartItem> cartItems = cartItemRepository
                .findAllByCart_IdOrderByCreatedAtAsc(
                        cart.getId()
                );

        if (cartItems.isEmpty()) {
            throw new InvalidRequestException(
                    "Cart is empty"
            );
        }

        List<CartItem> sortedCartItems = cartItems.stream()
                .sorted(Comparator.comparing(
                        item -> item.getBookEdition().getId()
                ))
                .toList();

        Map<Long, Inventory> inventoryByEditionId =
                lockInventories(sortedCartItems);

        validateCheckoutItems(
                sortedCartItems,
                inventoryByEditionId
        );

        BigDecimal subtotal = calculateSubtotal(
                sortedCartItems,
                inventoryByEditionId
        );

        Address address = createAddress(
                request.shippingAddress()
        );

        Order order = orderRepository.save(
                new Order(
                        user,
                        address,
                        subtotal,
                        SHIPPING_FEE
                )
        );

        List<OrderItem> orderItems = sortedCartItems.stream()
                .map(cartItem -> {
                    Long editionId =
                            cartItem.getBookEdition().getId();

                    Inventory inventory =
                            inventoryByEditionId.get(editionId);

                    return new OrderItem(
                            order,
                            cartItem.getBookEdition(),
                            inventory.getPrice(),
                            cartItem.getQuantity()
                    );
                })
                .toList();

        List<OrderItem> savedOrderItems =
                orderItemRepository.saveAll(orderItems);

        Payment payment = paymentRepository.save(
                new Payment(
                        order,
                        PaymentMethod.CASH_ON_DELIVERY,
                        order.getTotalAmount()
                )
        );

        for (CartItem cartItem : sortedCartItems) {
            Long editionId =
                    cartItem.getBookEdition().getId();

            inventoryByEditionId
                    .get(editionId)
                    .decreaseStock(cartItem.getQuantity());
        }

        inventoryRepository.saveAll(
                inventoryByEditionId.values()
        );

        cartItemRepository.deleteAllByCart_Id(
                cart.getId()
        );

        cart.markUpdated();
        cartRepository.save(cart);

        return orderResponseMapper.toCheckoutResponse(
                order,
                savedOrderItems,
                payment
        );
    }

    private AppUser getAuthenticatedUser(String subject) {
        Long userId;

        try {
            userId = Long.parseLong(subject);
        } catch (NumberFormatException exception) {
            throw new InvalidAccessTokenException();
        }

        return userRepository.findById(userId)
                .filter(AppUser::isEnabled)
                .orElseThrow(InvalidAccessTokenException::new);
    }

    private Map<Long, Inventory> lockInventories(
            List<CartItem> sortedCartItems
    ) {
        Map<Long, Inventory> inventoryByEditionId =
                new LinkedHashMap<>();

        for (CartItem cartItem : sortedCartItems) {
            Long editionId =
                    cartItem.getBookEdition().getId();

            Inventory inventory = inventoryRepository
                    .findByBookEditionIdForUpdate(editionId)
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Inventory for book edition with ID "
                                            + editionId
                                            + " was not found"
                            )
                    );

            inventoryByEditionId.put(
                    editionId,
                    inventory
            );
        }

        return inventoryByEditionId;
    }

    private void validateCheckoutItems(
            List<CartItem> cartItems,
            Map<Long, Inventory> inventoryByEditionId
    ) {
        for (CartItem cartItem : cartItems) {
            Long editionId =
                    cartItem.getBookEdition().getId();

            Integer quantity = cartItem.getQuantity();
            Inventory inventory =
                    inventoryByEditionId.get(editionId);

            if (quantity == null || quantity <= 0) {
                throw new InvalidRequestException(
                        "Invalid quantity for book edition with ID "
                                + editionId
                );
            }

            if (!inventory.isActive()
                    || inventory.getAvailabilityStatus()
                    != AvailabilityStatus.IN_STOCK
                    || inventory.getStockQuantity() <= 0) {

                throw new InvalidRequestException(
                        "Book edition with ID "
                                + editionId
                                + " is not available for checkout"
                );
            }

            if (quantity > inventory.getStockQuantity()) {
                throw new InvalidRequestException(
                        "Insufficient stock for book edition with ID "
                                + editionId
                                + ". Available quantity: "
                                + inventory.getStockQuantity()
                );
            }
        }
    }

    private BigDecimal calculateSubtotal(
            List<CartItem> cartItems,
            Map<Long, Inventory> inventoryByEditionId
    ) {
        return cartItems.stream()
                .map(cartItem -> {
                    Long editionId =
                            cartItem.getBookEdition().getId();

                    BigDecimal price =
                            inventoryByEditionId
                                    .get(editionId)
                                    .getPrice();

                    return price.multiply(
                            BigDecimal.valueOf(
                                    cartItem.getQuantity()
                            )
                    );
                })
                .reduce(
                        new BigDecimal("0.00"),
                        BigDecimal::add
                );
    }

    private Address createAddress(
            ShippingAddressRequest request
    ) {
        return new Address(
                request.recipientName(),
                request.phone(),
                request.addressLine1(),
                request.addressLine2(),
                request.city(),
                request.state(),
                request.postalCode(),
                request.country()
        );
    }
}