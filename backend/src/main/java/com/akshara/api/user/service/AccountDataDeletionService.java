package com.akshara.api.user.service;

import com.akshara.api.cart.repository.CartItemRepository;
import com.akshara.api.cart.repository.CartRepository;
import com.akshara.api.library.service.PersonalLibraryService;
import com.akshara.api.reading.repository.*;
import com.akshara.api.wishlist.repository.WishlistItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountDataDeletionService {
    private final ReadingEntryRepository entryRepository;
    private final ReadingActivityRepository activityRepository;
    private final ReadingAnnotationRepository annotationRepository;
    private final ReadingGoalRepository goalRepository;
    private final WishlistItemRepository wishlistRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final PersonalLibraryService personalLibraryService;

    public AccountDataDeletionService(
            ReadingEntryRepository entryRepository,
            ReadingActivityRepository activityRepository,
            ReadingAnnotationRepository annotationRepository,
            ReadingGoalRepository goalRepository,
            WishlistItemRepository wishlistRepository,
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            PersonalLibraryService personalLibraryService
    ) {
        this.entryRepository = entryRepository;
        this.activityRepository = activityRepository;
        this.annotationRepository = annotationRepository;
        this.goalRepository = goalRepository;
        this.wishlistRepository = wishlistRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.personalLibraryService = personalLibraryService;
    }

    @Transactional
    public void purgePrivateData(Long userId) {
        var entries = entryRepository.findAllByUser_IdOrderByUpdatedAtDesc(userId);
        entries.forEach(entry -> {
            annotationRepository.deleteAllByReadingEntry_Id(entry.getId());
            activityRepository.deleteAllByReadingEntry_Id(entry.getId());
        });
        entryRepository.deleteAll(entries);
        goalRepository.deleteAllByUser_Id(userId);
        wishlistRepository.deleteAllByUser_Id(userId);
        cartRepository.findByUser_Id(userId).ifPresent(cart -> {
            cartItemRepository.deleteAllByCart_Id(cart.getId());
            cartRepository.delete(cart);
        });
        personalLibraryService.deleteAllForUser(userId);
    }
}
