package com.akshara.api.cart.service;

import com.akshara.api.auth.exception.InvalidAccessTokenException;
import com.akshara.api.book.entity.AvailabilityStatus;
import com.akshara.api.book.entity.Book;
import com.akshara.api.book.entity.BookEdition;
import com.akshara.api.book.entity.Inventory;
import com.akshara.api.book.entity.Publisher;
import com.akshara.api.book.repository.BookEditionRepository;
import com.akshara.api.cart.dto.AddCartItemRequest;
import com.akshara.api.cart.dto.CartItemResponse;
import com.akshara.api.cart.dto.CartResponse;
import com.akshara.api.cart.dto.UpdateCartItemRequest;
import com.akshara.api.cart.entity.Cart;
import com.akshara.api.cart.entity.CartItem;
import com.akshara.api.cart.repository.CartItemRepository;
import com.akshara.api.cart.repository.CartRepository;
import com.akshara.api.common.exception.InvalidRequestException;
import com.akshara.api.common.exception.ResourceNotFoundException;
import com.akshara.api.user.entity.AppUser;
import com.akshara.api.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.akshara.api.book.repository.InventoryRepository;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final BookEditionRepository bookEditionRepository;
    private final InventoryRepository inventoryRepository;

    public CartService(
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            UserRepository userRepository,
            BookEditionRepository bookEditionRepository,
            InventoryRepository inventoryRepository
    ) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.userRepository = userRepository;
        this.bookEditionRepository = bookEditionRepository;
        this.inventoryRepository = inventoryRepository;
    }

    public CartResponse getCart(String subject) {
        AppUser user = getAuthenticatedUser(subject);
        Cart cart = getOrCreateCart(user);

        return toResponse(cart);
    }

    public CartResponse addItem(
            String subject,
            AddCartItemRequest request
    ) {
        AppUser user = getAuthenticatedUser(subject);
        Cart cart = getOrCreateCart(user);

        BookEdition edition = bookEditionRepository
                .findById(request.bookEditionId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Book edition with ID "
                                + request.bookEditionId()
                                + " was not found"
                ));

        if (edition.getFormat().isDigital()) {
            throw new InvalidRequestException(
                    "Digital editions are not available for purchase yet"
            );
        }

        CartItem item = cartItemRepository
                .findByCart_IdAndBookEdition_Id(
                        cart.getId(),
                        edition.getId()
                )
                .orElse(null);

        int existingQuantity = item == null
                ? 0
                : item.getQuantity();

        long requestedTotal =
                (long) existingQuantity + request.quantity();

        if (requestedTotal > Integer.MAX_VALUE) {
            throw new InvalidRequestException(
                    "Requested cart quantity is too large"
            );
        }

        Inventory inventory = getInventory(edition.getId());

        validatePurchasableQuantity(
                inventory,
                (int) requestedTotal
        );

        if (item == null) {
            item = new CartItem(
                    cart,
                    edition,
                    request.quantity()
            );
        } else {
            item.setQuantity((int) requestedTotal);
        }

        cartItemRepository.save(item);
        touchCart(cart);

        return toResponse(cart);
    }

    public CartResponse updateItem(
            String subject,
            Long itemId,
            UpdateCartItemRequest request
    ) {
        AppUser user = getAuthenticatedUser(subject);
        Cart cart = getOrCreateCart(user);

        CartItem item = findOwnedItem(cart, itemId);

        Inventory inventory = getInventory(
                item.getBookEdition().getId()
        );

        validatePurchasableQuantity(
                inventory,
                request.quantity()
        );

        item.setQuantity(request.quantity());

        cartItemRepository.save(item);
        touchCart(cart);

        return toResponse(cart);
    }

    public CartResponse removeItem(
            String subject,
            Long itemId
    ) {
        AppUser user = getAuthenticatedUser(subject);
        Cart cart = getOrCreateCart(user);

        CartItem item = findOwnedItem(cart, itemId);

        cartItemRepository.delete(item);
        touchCart(cart);

        return toResponse(cart);
    }

    public CartResponse clearCart(String subject) {
        AppUser user = getAuthenticatedUser(subject);
        Cart cart = getOrCreateCart(user);

        cartItemRepository.deleteAllByCart_Id(cart.getId());
        touchCart(cart);

        return toResponse(cart);
    }

    private AppUser getAuthenticatedUser(String subject) {
        Long userId;

        try {
            userId = Long.parseLong(subject);
        } catch (NumberFormatException exception) {
            throw new InvalidAccessTokenException();
        }

        return userRepository.findById(userId)
                .filter(AppUser::isEnabled)
                .orElseThrow(InvalidAccessTokenException::new);
    }

    private Cart getOrCreateCart(AppUser user) {
        return cartRepository.findByUser_Id(user.getId())
                .orElseGet(() ->
                        cartRepository.save(new Cart(user))
                );
    }

    private CartItem findOwnedItem(
            Cart cart,
            Long itemId
    ) {
        return cartItemRepository
                .findByIdAndCart_Id(itemId, cart.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cart item with ID "
                                + itemId
                                + " was not found"
                ));
    }

    private Inventory getInventory(Long editionId) {
        return inventoryRepository
                .findByBookEdition_Id(editionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Inventory for book edition with ID "
                                + editionId
                                + " was not found"
                ));
    }

    private void validatePurchasableQuantity(
            Inventory inventory,
            int quantity
    ) {
        if (!inventory.isActive()
                || inventory.getAvailabilityStatus()
                != AvailabilityStatus.IN_STOCK
                || inventory.getStockQuantity() <= 0) {

            throw new InvalidRequestException(
                    "This book edition is not currently available"
            );
        }

        if (quantity > inventory.getStockQuantity()) {
            throw new InvalidRequestException(
                    "Requested quantity exceeds the available stock of "
                            + inventory.getStockQuantity()
            );
        }
    }

    private void touchCart(Cart cart) {
        cart.markUpdated();
        cartRepository.save(cart);
    }

    private CartResponse toResponse(Cart cart) {
        List<CartItemResponse> items = cartItemRepository
                .findAllByCart_IdOrderByCreatedAtAsc(
                        cart.getId()
                )
                .stream()
                .map(this::toItemResponse)
                .toList();

        int totalQuantity = items.stream()
                .mapToInt(CartItemResponse::quantity)
                .sum();

        BigDecimal totalAmount = items.stream()
                .map(CartItemResponse::subtotal)
                .reduce(
                        BigDecimal.ZERO.setScale(2),
                        BigDecimal::add
                );

        return new CartResponse(
                cart.getId(),
                items,
                items.size(),
                totalQuantity,
                totalAmount,
                cart.getUpdatedAt()
        );
    }

    private CartItemResponse toItemResponse(CartItem item) {
        BookEdition edition = item.getBookEdition();
        Book book = edition.getBook();
        Publisher publisher = edition.getPublisher();
        Inventory inventory = getInventory(edition.getId());

        BigDecimal subtotal = inventory.getPrice()
                .multiply(
                        BigDecimal.valueOf(item.getQuantity())
                );

        boolean availableForPurchase =
                inventory.isActive()
                        && inventory.getAvailabilityStatus()
                        == AvailabilityStatus.IN_STOCK
                        && inventory.getStockQuantity()
                        >= item.getQuantity();

        return new CartItemResponse(
                item.getId(),

                book.getId(),
                book.getTitle(),
                book.getCoverImageUrl(),

                edition.getId(),
                edition.getFormat(),
                edition.getEditionName(),
                edition.getIsbn10(),
                edition.getIsbn13(),
                publisher == null
                        ? null
                        : publisher.getName(),

                inventory.getPrice(),
                item.getQuantity(),
                subtotal,

                inventory.getStockQuantity(),
                inventory.getAvailabilityStatus(),
                availableForPurchase
        );
    }
}
