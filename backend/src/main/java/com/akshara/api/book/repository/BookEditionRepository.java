package com.akshara.api.book.repository;

import com.akshara.api.book.entity.BookEdition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookEditionRepository
        extends JpaRepository<BookEdition, Long> {

    List<BookEdition> findAllByBook_Id(Long bookId);

    Optional<BookEdition> findByIsbn10(String isbn10);

    Optional<BookEdition> findByIsbn13(String isbn13);

    boolean existsByIsbn10(String isbn10);

    boolean existsByIsbn13(String isbn13);
}