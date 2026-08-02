package com.akshara.api.book.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;

import com.akshara.api.common.exception.InvalidRequestException;

@Entity
@Table(
        name = "inventory",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_inventory_book_edition",
                        columnNames = "book_edition_id"
                )
        },
        indexes = {
                @Index(
                        name = "idx_inventory_availability",
                        columnList = "availability_status"
                )
        }
)
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "book_edition_id",
            nullable = false,
            unique = true
    )
    private BookEdition bookEdition;

    @Column(
            nullable = false,
            precision = 10,
            scale = 2
    )
    private BigDecimal price;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "availability_status",
            nullable = false,
            length = 20
    )
    private AvailabilityStatus availabilityStatus;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Inventory() {
        // Required by JPA
    }

    public Inventory(
            BookEdition bookEdition,
            BigDecimal price,
            Integer stockQuantity,
            AvailabilityStatus availabilityStatus
    ) {
        this.bookEdition = bookEdition;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.availabilityStatus = availabilityStatus;
    }

    @PrePersist
    void beforeInsert() {
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    void beforeUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public BookEdition getBookEdition() {
        return bookEdition;
    }

    public void setBookEdition(BookEdition bookEdition) {
        this.bookEdition = bookEdition;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public AvailabilityStatus getAvailabilityStatus() {
        return availabilityStatus;
    }

    public void setAvailabilityStatus(
            AvailabilityStatus availabilityStatus
    ) {
        this.availabilityStatus = availabilityStatus;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void decreaseStock(int requestedQuantity) {
        if (requestedQuantity <= 0) {
            throw new InvalidRequestException(
                    "Checkout quantity must be greater than zero"
            );
        }

        if (!active) {
            throw new InvalidRequestException(
                    "Book edition is inactive"
            );
        }

        if (availabilityStatus != AvailabilityStatus.IN_STOCK) {
            throw new InvalidRequestException(
                    "Book edition is not available"
            );
        }

        if (requestedQuantity > stockQuantity) {
            throw new InvalidRequestException(
                    "Insufficient stock. Available quantity: "
                            + stockQuantity
            );
        }

        stockQuantity -= requestedQuantity;

        if (stockQuantity == 0) {
            availabilityStatus = AvailabilityStatus.OUT_OF_STOCK;
        }
    }

    public void increaseStock(int returnedQuantity) {
        if (returnedQuantity <= 0) {
            throw new InvalidRequestException(
                    "Returned quantity must be greater than zero"
            );
        }

        stockQuantity += returnedQuantity;

        if (availabilityStatus == AvailabilityStatus.OUT_OF_STOCK) {
            availabilityStatus = AvailabilityStatus.IN_STOCK;
        }
    }
}