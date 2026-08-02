package com.akshara.api.wishlist.service;

import com.akshara.api.book.entity.Book;
import com.akshara.api.book.repository.BookRepository;
import com.akshara.api.book.service.BookService;
import com.akshara.api.common.exception.ResourceNotFoundException;
import com.akshara.api.user.entity.AppUser;
import com.akshara.api.user.service.UserService;
import com.akshara.api.wishlist.dto.WishlistItemResponse;
import com.akshara.api.wishlist.entity.WishlistItem;
import com.akshara.api.wishlist.repository.WishlistItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class WishlistService {

    private final WishlistItemRepository wishlistItemRepository;
    private final BookRepository bookRepository;
    private final BookService bookService;
    private final UserService userService;

    public WishlistService(
            WishlistItemRepository wishlistItemRepository,
            BookRepository bookRepository,
            BookService bookService,
            UserService userService
    ) {
        this.wishlistItemRepository = wishlistItemRepository;
        this.bookRepository = bookRepository;
        this.bookService = bookService;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public List<WishlistItemResponse> getWishlist(String subject) {
        AppUser user = userService.getCurrentUserEntity(subject);

        return wishlistItemRepository
                .findAllByUser_IdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public WishlistItemResponse add(String subject, Long bookId) {
        AppUser user = userService.getCurrentUserEntity(subject);

        WishlistItem existing = wishlistItemRepository
                .findByUser_IdAndBook_Id(user.getId(), bookId)
                .orElse(null);

        if (existing != null) {
            return toResponse(existing);
        }

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Book not found with id: " + bookId
                ));

        return toResponse(wishlistItemRepository.save(new WishlistItem(user, book)));
    }

    public void remove(String subject, Long bookId) {
        AppUser user = userService.getCurrentUserEntity(subject);
        WishlistItem item = wishlistItemRepository
                .findByUser_IdAndBook_Id(user.getId(), bookId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Book is not present in this wishlist"
                ));

        wishlistItemRepository.delete(item);
    }

    private WishlistItemResponse toResponse(WishlistItem item) {
        return new WishlistItemResponse(
                item.getId(),
                bookService.getById(item.getBook().getId()),
                item.getCreatedAt()
        );
    }
}
