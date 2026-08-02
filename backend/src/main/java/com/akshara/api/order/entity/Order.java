package com.akshara.api.order.entity;

import com.akshara.api.user.entity.AppUser;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "orders",
        indexes = {
                @Index(
                        name = "idx_orders_user_created",
                        columnList = "user_id, placed_at"
                ),
                @Index(
                        name = "idx_orders_status",
                        columnList = "status"
                )
        }
)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @OneToOne(
            fetch = FetchType.LAZY,
            optional = false,
            cascade = CascadeType.PERSIST
    )
    @JoinColumn(
            name = "shipping_address_id",
            nullable = false,
            unique = true
    )
    private Address shippingAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status = OrderStatus.PLACED;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(
            name = "shipping_fee",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal shippingFee;

    @Column(
            name = "total_amount",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal totalAmount;

    @Column(
            name = "placed_at",
            nullable = false,
            updatable = false
    )
    private Instant placedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Order() {
        // Required by JPA
    }

    public Order(
            AppUser user,
            Address shippingAddress,
            BigDecimal subtotal,
            BigDecimal shippingFee
    ) {
        this.user = user;
        this.shippingAddress = shippingAddress;
        this.subtotal = subtotal;
        this.shippingFee = shippingFee;
        this.totalAmount = subtotal.add(shippingFee);
        this.status = OrderStatus.PLACED;
    }

    @PrePersist
    void beforeInsert() {
        Instant now = Instant.now();
        this.placedAt = now;
        this.updatedAt = now;

        if (status == null) {
            status = OrderStatus.PLACED;
        }
    }

    @PreUpdate
    void beforeUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public AppUser getUser() {
        return user;
    }

    public Address getShippingAddress() {
        return shippingAddress;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getShippingFee() {
        return shippingFee;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public Instant getPlacedAt() {
        return placedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }
}