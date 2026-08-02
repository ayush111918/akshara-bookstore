package com.akshara.api.order.entity;

import com.akshara.api.book.entity.Book;
import com.akshara.api.book.entity.BookEdition;
import com.akshara.api.book.entity.BookFormat;
import com.akshara.api.book.entity.Publisher;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(
        name = "order_items",
        indexes = {
                @Index(
                        name = "idx_order_items_order",
                        columnList = "order_id"
                ),
                @Index(
                        name = "idx_order_items_edition",
                        columnList = "book_edition_id"
                )
        }
)
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_edition_id", nullable = false)
    private BookEdition bookEdition;

    @Column(
            name = "book_title",
            nullable = false,
            length = 255,
            updatable = false
    )
    private String bookTitle;

    @Column(
            name = "cover_image_url",
            length = 1000,
            updatable = false
    )
    private String coverImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "book_format",
            nullable = false,
            length = 20,
            updatable = false
    )
    private BookFormat bookFormat;

    @Column(
            name = "edition_name",
            length = 100,
            updatable = false
    )
    private String editionName;

    @Column(length = 10, updatable = false)
    private String isbn10;

    @Column(length = 13, updatable = false)
    private String isbn13;

    @Column(
            name = "publisher_name",
            length = 255,
            updatable = false
    )
    private String publisherName;

    @Column(
            name = "unit_price",
            nullable = false,
            precision = 10,
            scale = 2,
            updatable = false
    )
    private BigDecimal unitPrice;

    @Column(nullable = false, updatable = false)
    private Integer quantity;

    @Column(
            nullable = false,
            precision = 12,
            scale = 2,
            updatable = false
    )
    private BigDecimal subtotal;

    protected OrderItem() {
        // Required by JPA
    }

    public OrderItem(
            Order order,
            BookEdition bookEdition,
            BigDecimal unitPrice,
            Integer quantity
    ) {
        Book book = bookEdition.getBook();
        Publisher publisher = bookEdition.getPublisher();

        this.order = order;
        this.bookEdition = bookEdition;

        this.bookTitle = book.getTitle();
        this.coverImageUrl = book.getCoverImageUrl();
        this.bookFormat = bookEdition.getFormat();
        this.editionName = bookEdition.getEditionName();
        this.isbn10 = bookEdition.getIsbn10();
        this.isbn13 = bookEdition.getIsbn13();
        this.publisherName =
                publisher == null ? null : publisher.getName();

        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.subtotal = unitPrice.multiply(
                BigDecimal.valueOf(quantity)
        );
    }

    public Long getId() {
        return id;
    }

    public Order getOrder() {
        return order;
    }

    public BookEdition getBookEdition() {
        return bookEdition;
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public BookFormat getBookFormat() {
        return bookFormat;
    }

    public String getEditionName() {
        return editionName;
    }

    public String getIsbn10() {
        return isbn10;
    }

    public String getIsbn13() {
        return isbn13;
    }

    public String getPublisherName() {
        return publisherName;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }
}