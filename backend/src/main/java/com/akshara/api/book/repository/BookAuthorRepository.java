package com.akshara.api.book.repository;

import com.akshara.api.book.entity.BookAuthor;
import com.akshara.api.book.entity.BookAuthorId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookAuthorRepository
        extends JpaRepository<BookAuthor, BookAuthorId> {

    List<BookAuthor> findAllByBook_Id(Long bookId);

    List<BookAuthor> findAllByAuthor_Id(Long authorId);

    boolean existsByBook_IdAndAuthor_Id(
            Long bookId,
            Long authorId
    );

    void deleteByBook_IdAndAuthor_Id(
            Long bookId,
            Long authorId
    );
}