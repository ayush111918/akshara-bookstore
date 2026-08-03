package com.akshara.api.book.service;

import com.akshara.api.book.entity.Book;
import com.akshara.api.book.repository.BookRepository;
import com.akshara.api.cart.repository.CartItemRepository;
import com.akshara.api.order.repository.OrderItemRepository;
import com.akshara.api.review.repository.ReviewRepository;
import com.akshara.api.wishlist.repository.WishlistItemRepository;
import org.springframework.stereotype.Component;

@Component
class BookDeletionManager {

    private final BookRepository books;
    private final CartItemRepository cartItems;
    private final WishlistItemRepository wishlistItems;
    private final ReviewRepository reviews;
    private final OrderItemRepository orderItems;
    private final BookRelationshipManager relationships;
    private final BookEditionManager editions;

    BookDeletionManager(
            BookRepository books,
            CartItemRepository cartItems,
            WishlistItemRepository wishlistItems,
            ReviewRepository reviews,
            OrderItemRepository orderItems,
            BookRelationshipManager relationships,
            BookEditionManager editions
    ) {
        this.books = books;
        this.cartItems = cartItems;
        this.wishlistItems = wishlistItems;
        this.reviews = reviews;
        this.orderItems = orderItems;
        this.relationships = relationships;
        this.editions = editions;
    }

    void delete(Book book) {
        Long bookId = book.getId();
        cartItems.deleteAllByBookEdition_Book_Id(bookId);
        cartItems.flush();
        wishlistItems.deleteAllByBook_Id(bookId);
        wishlistItems.flush();
        reviews.deleteAllByBook_Id(bookId);
        reviews.flush();
        orderItems.detachAllByBookId(bookId);
        orderItems.flush();
        editions.deleteForBook(bookId);
        relationships.deleteForBook(bookId);
        books.delete(book);
    }
}
