package com.akshara.api.user.service;

import com.akshara.api.cart.entity.Cart;
import com.akshara.api.cart.repository.CartItemRepository;
import com.akshara.api.cart.repository.CartRepository;
import com.akshara.api.library.service.PersonalLibraryService;
import com.akshara.api.reading.entity.ReadingEntry;
import com.akshara.api.reading.repository.*;
import com.akshara.api.wishlist.repository.WishlistItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountDataDeletionServiceTest {
    @Mock ReadingEntryRepository entryRepository;
    @Mock ReadingActivityRepository activityRepository;
    @Mock ReadingAnnotationRepository annotationRepository;
    @Mock ReadingGoalRepository goalRepository;
    @Mock WishlistItemRepository wishlistRepository;
    @Mock CartRepository cartRepository;
    @Mock CartItemRepository cartItemRepository;
    @Mock PersonalLibraryService personalLibraryService;

    @Test
    void purgeShouldRemovePrivateReaderDataInDependencyOrder() {
        ReadingEntry entry = mock(ReadingEntry.class);
        Cart cart = mock(Cart.class);
        when(entry.getId()).thenReturn(9L);
        when(cart.getId()).thenReturn(4L);
        when(entryRepository.findAllByUser_IdOrderByUpdatedAtDesc(7L)).thenReturn(List.of(entry));
        when(cartRepository.findByUser_Id(7L)).thenReturn(Optional.of(cart));

        var service = new AccountDataDeletionService(entryRepository, activityRepository,
                annotationRepository, goalRepository, wishlistRepository, cartRepository,
                cartItemRepository, personalLibraryService);
        service.purgePrivateData(7L);

        verify(annotationRepository).deleteAllByReadingEntry_Id(9L);
        verify(activityRepository).deleteAllByReadingEntry_Id(9L);
        verify(entryRepository).deleteAll(List.of(entry));
        verify(goalRepository).deleteAllByUser_Id(7L);
        verify(wishlistRepository).deleteAllByUser_Id(7L);
        verify(cartItemRepository).deleteAllByCart_Id(4L);
        verify(cartRepository).delete(cart);
        verify(personalLibraryService).deleteAllForUser(7L);
    }
}
