package com.akshara.api.cart.entity;

import com.akshara.api.book.entity.BookEdition;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(
        name = "cart_items",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_cart_items_cart_edition",
                        columnNames = {
                                "cart_id",
                                "book_edition_id"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_cart_items_cart",
                        columnList = "cart_id"
                ),
                @Index(
                        name = "idx_cart_items_edition",
                        columnList = "book_edition_id"
                )
        }
)
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "book_edition_id",
            nullable = false
    )
    private BookEdition bookEdition;

    @Column(nullable = false)
    private Integer quantity;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CartItem() {
        // Required by JPA
    }

    public CartItem(
            Cart cart,
            BookEdition bookEdition,
            Integer quantity
    ) {
        this.cart = cart;
        this.bookEdition = bookEdition;
        this.quantity = quantity;
    }

    @PrePersist
    void beforeInsert() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void beforeUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Cart getCart() {
        return cart;
    }

    public BookEdition getBookEdition() {
        return bookEdition;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setCart(Cart cart) {
        this.cart = cart;
    }

    public void setBookEdition(BookEdition bookEdition) {
        this.bookEdition = bookEdition;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}