package com.akshara.api.order.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "payments",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_payments_order",
                        columnNames = "order_id"
                )
        },
        indexes = {
                @Index(
                        name = "idx_payments_status",
                        columnList = "status"
                )
        }
)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "order_id",
            nullable = false,
            unique = true
    )
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Payment() {
        // Required by JPA
    }

    public Payment(
            Order order,
            PaymentMethod method,
            BigDecimal amount
    ) {
        this.order = order;
        this.method = method;
        this.amount = amount;
        this.status = PaymentStatus.PENDING;
    }

    @PrePersist
    void beforeInsert() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;

        if (status == null) {
            status = PaymentStatus.PENDING;
        }
    }

    @PreUpdate
    void beforeUpdate() {
        this.updatedAt = Instant.now();
    }

    public void markPaid() {
        this.status = PaymentStatus.PAID;
        this.paidAt = Instant.now();
    }

    public void markFailed() {
        this.status = PaymentStatus.FAILED;
    }

    public void cancel() {
        this.status = PaymentStatus.CANCELLED;
    }

    public Long getId() {
        return id;
    }

    public Order getOrder() {
        return order;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}