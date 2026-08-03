package com.akshara.api.cart.service;

import com.akshara.api.book.repository.BookEditionRepository;
import com.akshara.api.cart.dto.AddCartItemRequest;
import com.akshara.api.cart.dto.CartResponse;
import com.akshara.api.cart.dto.UpdateCartItemRequest;
import com.akshara.api.cart.entity.Cart;
import com.akshara.api.cart.entity.CartItem;
import com.akshara.api.cart.repository.CartItemRepository;
import com.akshara.api.cart.repository.CartRepository;
import com.akshara.api.common.exception.InvalidRequestException;
import com.akshara.api.common.exception.ResourceNotFoundException;
import com.akshara.api.user.entity.AppUser;
import com.akshara.api.user.entity.Role;
import com.akshara.api.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.akshara.api.book.repository.InventoryRepository;
import com.akshara.api.book.entity.AvailabilityStatus;
import com.akshara.api.book.entity.Inventory;
import com.akshara.api.book.entity.Book;
import com.akshara.api.book.entity.BookEdition;
import com.akshara.api.book.entity.BookFormat;


@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long CART_ID = 10L;
    private static final Long BOOK_ID = 20L;
    private static final Long EDITION_ID = 30L;
    private static final Long ITEM_ID = 40L;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BookEditionRepository bookEditionRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    private CartService cartService;

    private AppUser user;
    private Cart cart;
    private BookEdition edition;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        cartService = new CartService(
                cartRepository,
                cartItemRepository,
                userRepository,
                bookEditionRepository,
                inventoryRepository
        );

        user = new AppUser(
                "Cart Test Reader",
                "cart.reader@example.com",
                "password-hash",
                Role.READER
        );
        ReflectionTestUtils.setField(user, "id", USER_ID);

        cart = new Cart(user);
        ReflectionTestUtils.setField(cart, "id", CART_ID);

        Book book = new Book("Clean Code");
        ReflectionTestUtils.setField(book, "id", BOOK_ID);

        edition = new BookEdition(
                book,
                BookFormat.PAPERBACK
        );
        edition.setEditionName("First Edition");
        edition.setIsbn13("9780132350884");
        ReflectionTestUtils.setField(
                edition,
                "id",
                EDITION_ID
        );

        inventory = new Inventory(
                edition,
                new BigDecimal("299.00"),
                5,
                AvailabilityStatus.IN_STOCK
        );
    }

    @Test
    void addItemCreatesNewItemAndCalculatesTotals() {
        stubAuthenticatedCart();

        when(bookEditionRepository.findById(EDITION_ID))
                .thenReturn(Optional.of(edition));

        when(cartItemRepository
                .findByCart_IdAndBookEdition_Id(
                        CART_ID,
                        EDITION_ID
                ))
                .thenReturn(Optional.empty());

        when(inventoryRepository
                .findByBookEdition_Id(EDITION_ID))
                .thenReturn(Optional.of(inventory));

        AtomicReference<CartItem> savedItem =
                new AtomicReference<>();

        when(cartItemRepository.save(any(CartItem.class)))
                .thenAnswer(invocation -> {
                    CartItem item = invocation.getArgument(0);
                    ReflectionTestUtils.setField(
                            item,
                            "id",
                            ITEM_ID
                    );
                    savedItem.set(item);
                    return item;
                });

        when(cartItemRepository
                .findAllByCart_IdOrderByCreatedAtAsc(CART_ID))
                .thenAnswer(invocation ->
                        List.of(savedItem.get())
                );

        CartResponse response = cartService.addItem(
                USER_ID.toString(),
                new AddCartItemRequest(EDITION_ID, 2)
        );

        assertThat(savedItem.get().getQuantity()).isEqualTo(2);
        assertThat(response.distinctItems()).isEqualTo(1);
        assertThat(response.totalQuantity()).isEqualTo(2);
        assertThat(response.totalAmount())
                .isEqualByComparingTo("598.00");
        assertThat(response.items().get(0).subtotal())
                .isEqualByComparingTo("598.00");

        verify(cartItemRepository).save(savedItem.get());
        verify(cartRepository).save(cart);
    }

    @Test
    void addItemMergesQuantityForExistingEdition() {
        stubAuthenticatedCart();

        CartItem existingItem =
                new CartItem(cart, edition, 2);

        ReflectionTestUtils.setField(
                existingItem,
                "id",
                ITEM_ID
        );

        when(bookEditionRepository.findById(EDITION_ID))
                .thenReturn(Optional.of(edition));

        when(cartItemRepository
                .findByCart_IdAndBookEdition_Id(
                        CART_ID,
                        EDITION_ID
                ))
                .thenReturn(Optional.of(existingItem));

        when(inventoryRepository
                .findByBookEdition_Id(EDITION_ID))
                .thenReturn(Optional.of(inventory));

        when(cartItemRepository.save(existingItem))
                .thenReturn(existingItem);

        when(cartItemRepository
                .findAllByCart_IdOrderByCreatedAtAsc(CART_ID))
                .thenReturn(List.of(existingItem));

        CartResponse response = cartService.addItem(
                USER_ID.toString(),
                new AddCartItemRequest(EDITION_ID, 2)
        );

        assertThat(existingItem.getQuantity()).isEqualTo(4);
        assertThat(response.distinctItems()).isEqualTo(1);
        assertThat(response.totalQuantity()).isEqualTo(4);
        assertThat(response.totalAmount())
                .isEqualByComparingTo("1196.00");

        verify(cartItemRepository).save(existingItem);
    }

    @Test
    void addItemRejectsQuantityGreaterThanStock() {
        stubAuthenticatedCart();

        CartItem existingItem =
                new CartItem(cart, edition, 3);

        ReflectionTestUtils.setField(
                existingItem,
                "id",
                ITEM_ID
        );

        when(bookEditionRepository.findById(EDITION_ID))
                .thenReturn(Optional.of(edition));

        when(cartItemRepository
                .findByCart_IdAndBookEdition_Id(
                        CART_ID,
                        EDITION_ID
                ))
                .thenReturn(Optional.of(existingItem));

        when(inventoryRepository
                .findByBookEdition_Id(EDITION_ID))
                .thenReturn(Optional.of(inventory));

        assertThatThrownBy(() ->
                cartService.addItem(
                        USER_ID.toString(),
                        new AddCartItemRequest(EDITION_ID, 3)
                )
        )
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining(
                        "exceeds the available stock of 5"
                );

        assertThat(existingItem.getQuantity()).isEqualTo(3);

        verify(
                cartItemRepository,
                never()
        ).save(any(CartItem.class));

        verify(
                cartRepository,
                never()
        ).save(any(Cart.class));
    }

    @Test
    void addItemRejectsDigitalEditionWithoutDeliveryEntitlement() {
        stubAuthenticatedCart();
        edition.setFormat(BookFormat.EPUB);
        when(bookEditionRepository.findById(EDITION_ID))
                .thenReturn(Optional.of(edition));

        assertThatThrownBy(() ->
                cartService.addItem(
                        USER_ID.toString(),
                        new AddCartItemRequest(EDITION_ID, 1)
                )
        )
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Digital editions are not available for purchase yet");

        verify(inventoryRepository, never())
                .findByBookEdition_Id(any());
        verify(cartItemRepository, never())
                .save(any(CartItem.class));
    }

    @Test
    void updateItemRejectsItemNotOwnedByReader() {
        stubAuthenticatedCart();

        when(cartItemRepository.findByIdAndCart_Id(
                ITEM_ID,
                CART_ID
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                cartService.updateItem(
                        USER_ID.toString(),
                        ITEM_ID,
                        new UpdateCartItemRequest(1)
                )
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(
                        "Cart item with ID 40 was not found"
                );

        verify(
                inventoryRepository,
                never()
        ).findByBookEdition_Id(any());

        verify(
                cartItemRepository,
                never()
        ).save(any(CartItem.class));
    }

    @Test
    void clearCartDeletesEveryItemAndReturnsEmptyCart() {
        stubAuthenticatedCart();

        when(cartItemRepository
                .findAllByCart_IdOrderByCreatedAtAsc(CART_ID))
                .thenReturn(List.of());

        CartResponse response = cartService.clearCart(
                USER_ID.toString()
        );

        verify(cartItemRepository)
                .deleteAllByCart_Id(CART_ID);

        verify(cartRepository).save(cart);

        assertThat(response.distinctItems()).isZero();
        assertThat(response.totalQuantity()).isZero();
        assertThat(response.totalAmount())
                .isEqualByComparingTo("0.00");
        assertThat(response.items()).isEmpty();
    }

    private void stubAuthenticatedCart() {
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));

        when(cartRepository.findByUser_Id(USER_ID))
                .thenReturn(Optional.of(cart));
    }
}
