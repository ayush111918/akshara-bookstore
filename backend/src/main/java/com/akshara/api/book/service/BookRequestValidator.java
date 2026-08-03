package com.akshara.api.book.service;

import com.akshara.api.book.dto.BookEditionRequest;
import com.akshara.api.book.entity.BookFormat;
import com.akshara.api.book.repository.BookEditionRepository;
import com.akshara.api.common.exception.DuplicateResourceException;
import com.akshara.api.common.exception.InvalidRequestException;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
class BookRequestValidator {

    private final BookEditionRepository editionRepository;

    BookRequestValidator(BookEditionRepository editionRepository) {
        this.editionRepository = editionRepository;
    }

    void validate(List<BookEditionRequest> editions, Long currentBookId) {
        validateFormats(editions);
        validateIsbns(editions, currentBookId);
        validateSkus(editions, currentBookId);
    }

    void rejectUnsupportedDigitalSale(BookFormat format, boolean active) {
        if (active && format.isDigital()) {
            throw new InvalidRequestException(
                    "Digital editions cannot be activated for sale until "
                            + "a licensed file and reader entitlement are configured"
            );
        }
    }

    String normalizeIsbn10(String value) {
        String normalized = normalizeOptional(value);
        return normalized == null ? null
                : normalized.replaceAll("[-\\s]", "").toUpperCase(Locale.ROOT);
    }

    String normalizeIsbn13(String value) {
        String normalized = normalizeOptional(value);
        return normalized == null ? null : normalized.replaceAll("[-\\s]", "");
    }

    String normalizeOptional(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void validateFormats(List<BookEditionRequest> editions) {
        editions.forEach(edition -> rejectUnsupportedDigitalSale(
                edition.format(), edition.inventory().active()
        ));
    }

    private void validateIsbns(List<BookEditionRequest> editions, Long currentBookId) {
        Set<String> isbn10Values = new HashSet<>();
        Set<String> isbn13Values = new HashSet<>();
        for (BookEditionRequest edition : editions) {
            validateIsbn10(normalizeIsbn10(edition.isbn10()), currentBookId, isbn10Values);
            validateIsbn13(normalizeIsbn13(edition.isbn13()), currentBookId, isbn13Values);
        }
    }

    private void validateIsbn10(String isbn, Long currentBookId, Set<String> requestValues) {
        if (isbn == null) return;
        if (!isbn.matches("^[0-9]{9}[0-9X]$") || !validIsbn10Checksum(isbn)) {
            throw new InvalidRequestException("ISBN-10 '" + isbn + "' is invalid");
        }
        rejectRequestDuplicate(requestValues, isbn, "ISBN-10");
        editionRepository.findByIsbn10(isbn)
                .filter(existing -> belongsToAnotherBook(existing.getBook().getId(), currentBookId))
                .ifPresent(existing -> { throw duplicateExisting("ISBN-10", isbn); });
    }

    private void validateIsbn13(String isbn, Long currentBookId, Set<String> requestValues) {
        if (isbn == null) return;
        if (!isbn.matches("^[0-9]{13}$") || !validIsbn13Checksum(isbn)) {
            throw new InvalidRequestException("ISBN-13 '" + isbn + "' is invalid");
        }
        rejectRequestDuplicate(requestValues, isbn, "ISBN-13");
        editionRepository.findByIsbn13(isbn)
                .filter(existing -> belongsToAnotherBook(existing.getBook().getId(), currentBookId))
                .ifPresent(existing -> { throw duplicateExisting("ISBN-13", isbn); });
    }

    private void validateSkus(List<BookEditionRequest> editions, Long currentBookId) {
        Set<String> requestValues = new HashSet<>();
        for (BookEditionRequest edition : editions) {
            String sku = edition.sku().trim();
            rejectRequestDuplicate(requestValues, sku.toLowerCase(Locale.ROOT), "SKU", sku);
            editionRepository.findBySkuIgnoreCase(sku)
                    .filter(existing -> belongsToAnotherBook(existing.getBook().getId(), currentBookId))
                    .ifPresent(existing -> { throw duplicateExisting("SKU", sku); });
        }
    }

    private boolean belongsToAnotherBook(Long existingBookId, Long currentBookId) {
        return currentBookId == null || !existingBookId.equals(currentBookId);
    }

    private void rejectRequestDuplicate(Set<String> values, String value, String label) {
        rejectRequestDuplicate(values, value, label, value);
    }

    private void rejectRequestDuplicate(Set<String> values, String value, String label, String display) {
        if (!values.add(value)) {
            throw new DuplicateResourceException(
                    label + " '" + display + "' occurs more than once in the request"
            );
        }
    }

    private DuplicateResourceException duplicateExisting(String label, String value) {
        return new DuplicateResourceException(
                "A book edition with " + label + " '" + value + "' already exists"
        );
    }

    private boolean validIsbn10Checksum(String isbn) {
        int sum = 0;
        for (int index = 0; index < 10; index++) {
            char character = isbn.charAt(index);
            sum += (10 - index) * (character == 'X' ? 10 : character - '0');
        }
        return sum % 11 == 0;
    }

    private boolean validIsbn13Checksum(String isbn) {
        int sum = 0;
        for (int index = 0; index < 12; index++) {
            sum += (isbn.charAt(index) - '0') * (index % 2 == 0 ? 1 : 3);
        }
        return (10 - sum % 10) % 10 == isbn.charAt(12) - '0';
    }
}
