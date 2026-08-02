package com.akshara.api.book.specification;

import com.akshara.api.book.dto.BookSearchCriteria;
import com.akshara.api.book.entity.AvailabilityStatus;
import com.akshara.api.book.entity.Book;
import com.akshara.api.book.entity.BookAuthor;
import com.akshara.api.book.entity.BookCategory;
import com.akshara.api.book.entity.BookEdition;
import com.akshara.api.book.entity.Inventory;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class BookSpecifications {

    private BookSpecifications() {
        // Utility class
    }

    public static Specification<Book> withCriteria(
            BookSearchCriteria criteria
    ) {
        return (root, query, builder) -> {
            if (criteria == null) {
                return builder.conjunction();
            }

            List<Predicate> predicates = new ArrayList<>();

            addTextSearch(
                    predicates,
                    root,
                    query,
                    builder,
                    criteria.query()
            );

            if (criteria.authorId() != null) {
                predicates.add(hasAuthor(
                        root,
                        query,
                        builder,
                        criteria.authorId()
                ));
            }

            if (criteria.categoryId() != null) {
                predicates.add(hasCategory(
                        root,
                        query,
                        builder,
                        criteria.categoryId()
                ));
            }

            String languageCode = normalize(
                    criteria.languageCode()
            );

            if (languageCode != null) {
                predicates.add(builder.equal(
                        builder.lower(
                                root.<String>get("languageCode")
                        ),
                        languageCode.toLowerCase(Locale.ROOT)
                ));
            }

            if (hasEditionFilters(criteria)) {
                predicates.add(matchesEditionFilters(
                        root,
                        query,
                        builder,
                        criteria
                ));
            }

            return builder.and(
                    predicates.toArray(Predicate[]::new)
            );
        };
    }

    private static void addTextSearch(
            List<Predicate> predicates,
            Root<Book> root,
            CriteriaQuery<?> query,
            CriteriaBuilder builder,
            String searchValue
    ) {
        String normalizedSearch = normalize(searchValue);

        if (normalizedSearch == null) {
            return;
        }

        String pattern = "%"
                + escapeLike(
                normalizedSearch.toLowerCase(Locale.ROOT)
        )
                + "%";

        Predicate titleMatches = builder.like(
                builder.lower(root.<String>get("title")),
                pattern,
                '\\'
        );

        Predicate subtitleMatches = builder.like(
                builder.lower(root.<String>get("subtitle")),
                pattern,
                '\\'
        );

        Predicate authorMatches = authorNameMatches(
                root,
                query,
                builder,
                pattern
        );

        Predicate categoryMatches = categoryNameMatches(
                root,
                query,
                builder,
                pattern
        );

        Predicate isbnMatches = isbnMatches(
                root,
                query,
                builder,
                pattern
        );

        predicates.add(builder.or(
                titleMatches,
                subtitleMatches,
                authorMatches,
                categoryMatches,
                isbnMatches
        ));
    }

    private static Predicate authorNameMatches(
            Root<Book> root,
            CriteriaQuery<?> query,
            CriteriaBuilder builder,
            String pattern
    ) {
        Subquery<Long> subquery = query.subquery(Long.class);
        Root<BookAuthor> bookAuthor =
                subquery.from(BookAuthor.class);

        Path<Long> linkedBookId =
                bookAuthor.get("book").get("id");

        Path<String> authorName =
                bookAuthor.get("author").get("name");

        subquery.select(linkedBookId);
        subquery.where(
                builder.equal(
                        linkedBookId,
                        root.<Long>get("id")
                ),
                builder.like(
                        builder.lower(authorName),
                        pattern,
                        '\\'
                )
        );

        return builder.exists(subquery);
    }

    private static Predicate categoryNameMatches(
            Root<Book> root,
            CriteriaQuery<?> query,
            CriteriaBuilder builder,
            String pattern
    ) {
        Subquery<Long> subquery = query.subquery(Long.class);
        Root<BookCategory> bookCategory =
                subquery.from(BookCategory.class);

        Path<Long> linkedBookId =
                bookCategory.get("book").get("id");

        Path<String> categoryName =
                bookCategory.get("category").get("name");

        subquery.select(linkedBookId);
        subquery.where(
                builder.equal(
                        linkedBookId,
                        root.<Long>get("id")
                ),
                builder.like(
                        builder.lower(categoryName),
                        pattern,
                        '\\'
                )
        );

        return builder.exists(subquery);
    }

    private static Predicate isbnMatches(
            Root<Book> root,
            CriteriaQuery<?> query,
            CriteriaBuilder builder,
            String pattern
    ) {
        Subquery<Long> subquery = query.subquery(Long.class);
        Root<BookEdition> edition =
                subquery.from(BookEdition.class);

        Path<Long> editionId = edition.get("id");
        Path<Long> editionBookId =
                edition.get("book").get("id");

        Predicate isbn10Matches = builder.like(
                builder.lower(edition.<String>get("isbn10")),
                pattern,
                '\\'
        );

        Predicate isbn13Matches = builder.like(
                builder.lower(edition.<String>get("isbn13")),
                pattern,
                '\\'
        );

        subquery.select(editionId);
        subquery.where(
                builder.equal(
                        editionBookId,
                        root.<Long>get("id")
                ),
                builder.or(
                        isbn10Matches,
                        isbn13Matches
                )
        );

        return builder.exists(subquery);
    }

    private static Predicate hasAuthor(
            Root<Book> root,
            CriteriaQuery<?> query,
            CriteriaBuilder builder,
            Long authorId
    ) {
        Subquery<Long> subquery = query.subquery(Long.class);
        Root<BookAuthor> bookAuthor =
                subquery.from(BookAuthor.class);

        Path<Long> linkedBookId =
                bookAuthor.get("book").get("id");

        Path<Long> linkedAuthorId =
                bookAuthor.get("author").get("id");

        subquery.select(linkedBookId);
        subquery.where(
                builder.equal(
                        linkedBookId,
                        root.<Long>get("id")
                ),
                builder.equal(linkedAuthorId, authorId)
        );

        return builder.exists(subquery);
    }

    private static Predicate hasCategory(
            Root<Book> root,
            CriteriaQuery<?> query,
            CriteriaBuilder builder,
            Long categoryId
    ) {
        Subquery<Long> subquery = query.subquery(Long.class);
        Root<BookCategory> bookCategory =
                subquery.from(BookCategory.class);

        Path<Long> linkedBookId =
                bookCategory.get("book").get("id");

        Path<Long> linkedCategoryId =
                bookCategory.get("category").get("id");

        subquery.select(linkedBookId);
        subquery.where(
                builder.equal(
                        linkedBookId,
                        root.<Long>get("id")
                ),
                builder.equal(linkedCategoryId, categoryId)
        );

        return builder.exists(subquery);
    }

    private static Predicate matchesEditionFilters(
            Root<Book> root,
            CriteriaQuery<?> query,
            CriteriaBuilder builder,
            BookSearchCriteria criteria
    ) {
        Subquery<Long> subquery = query.subquery(Long.class);
        Root<BookEdition> edition =
                subquery.from(BookEdition.class);

        Path<Long> editionId = edition.get("id");
        Path<Long> editionBookId =
                edition.get("book").get("id");

        List<Predicate> predicates = new ArrayList<>();

        predicates.add(builder.equal(
                editionBookId,
                root.<Long>get("id")
        ));

        if (criteria.format() != null) {
            predicates.add(builder.equal(
                    edition.get("format"),
                    criteria.format()
            ));
        }

        boolean inventoryRequired =
                criteria.minPrice() != null
                        || criteria.maxPrice() != null
                        || criteria.inStock();

        if (inventoryRequired) {
            Root<Inventory> inventory =
                    subquery.from(Inventory.class);

            Path<Long> inventoryEditionId =
                    inventory.get("bookEdition").get("id");

            predicates.add(builder.equal(
                    inventoryEditionId,
                    editionId
            ));

            predicates.add(builder.isTrue(
                    inventory.<Boolean>get("active")
            ));

            addPricePredicates(
                    predicates,
                    inventory,
                    builder,
                    criteria.minPrice(),
                    criteria.maxPrice()
            );

            if (criteria.inStock()) {
                predicates.add(builder.equal(
                        inventory.get("availabilityStatus"),
                        AvailabilityStatus.IN_STOCK
                ));

                predicates.add(builder.greaterThan(
                        inventory.<Integer>get("stockQuantity"),
                        0
                ));
            }
        }

        subquery.select(editionId);
        subquery.where(
                predicates.toArray(Predicate[]::new)
        );

        return builder.exists(subquery);
    }

    private static void addPricePredicates(
            List<Predicate> predicates,
            Root<Inventory> inventory,
            CriteriaBuilder builder,
            BigDecimal minPrice,
            BigDecimal maxPrice
    ) {
        Path<BigDecimal> price = inventory.get("price");

        if (minPrice != null) {
            predicates.add(
                    builder.greaterThanOrEqualTo(
                            price,
                            minPrice
                    )
            );
        }

        if (maxPrice != null) {
            predicates.add(
                    builder.lessThanOrEqualTo(
                            price,
                            maxPrice
                    )
            );
        }
    }

    private static boolean hasEditionFilters(
            BookSearchCriteria criteria
    ) {
        return criteria.format() != null
                || criteria.minPrice() != null
                || criteria.maxPrice() != null
                || criteria.inStock();
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String escapeLike(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}