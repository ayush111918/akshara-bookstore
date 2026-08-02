package com.akshara.api.book.repository;

import com.akshara.api.book.entity.BookCategory;
import com.akshara.api.book.entity.BookCategoryId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookCategoryRepository
        extends JpaRepository<BookCategory, BookCategoryId> {

    List<BookCategory> findAllByBook_Id(Long bookId);

    List<BookCategory> findAllByCategory_Id(Long categoryId);

    boolean existsByBook_IdAndCategory_Id(
            Long bookId,
            Long categoryId
    );

    void deleteByBook_IdAndCategory_Id(
            Long bookId,
            Long categoryId
    );
}