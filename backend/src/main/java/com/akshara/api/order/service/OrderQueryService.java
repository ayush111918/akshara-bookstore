package com.akshara.api.order.service;

import com.akshara.api.auth.exception.InvalidAccessTokenException;
import com.akshara.api.common.exception.ResourceNotFoundException;
import com.akshara.api.order.dto.CheckoutResponse;
import com.akshara.api.order.dto.OrderSummaryResponse;
import com.akshara.api.order.entity.Order;
import com.akshara.api.order.entity.OrderItem;
import com.akshara.api.order.entity.Payment;
import com.akshara.api.order.repository.OrderItemRepository;
import com.akshara.api.order.repository.OrderRepository;
import com.akshara.api.order.repository.PaymentRepository;
import com.akshara.api.user.entity.AppUser;
import com.akshara.api.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrderQueryService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final OrderResponseMapper orderResponseMapper;

    public OrderQueryService(
            UserRepository userRepository,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            PaymentRepository paymentRepository,
            OrderResponseMapper orderResponseMapper
    ) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.paymentRepository = paymentRepository;
        this.orderResponseMapper = orderResponseMapper;
    }

    @Transactional(readOnly = true)
    public List<OrderSummaryResponse> getOrders(
            String subject
    ) {
        AppUser user = getAuthenticatedUser(subject);

        return orderRepository
                .findAllByUser_IdOrderByPlacedAtDesc(user.getId())
                .stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CheckoutResponse getOrder(
            String subject,
            Long orderId
    ) {
        AppUser user = getAuthenticatedUser(subject);

        Order order = orderRepository
                .findByIdAndUser_Id(orderId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order with ID "
                                + orderId
                                + " was not found"
                ));

        List<OrderItem> orderItems = orderItemRepository
                .findAllByOrder_IdOrderByIdAsc(orderId);

        Payment payment = paymentRepository
                .findByOrder_Id(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment for order with ID "
                                + orderId
                                + " was not found"
                ));

        return orderResponseMapper.toCheckoutResponse(
                order,
                orderItems,
                payment
        );
    }

    private OrderSummaryResponse toSummaryResponse(
            Order order
    ) {
        return new OrderSummaryResponse(
                order.getId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getPlacedAt()
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
}