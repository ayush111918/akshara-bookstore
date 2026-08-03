package com.akshara.api.book.repository;

import com.akshara.api.book.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface BookRepository
        extends JpaRepository<Book, Long>,
        JpaSpecificationExecutor<Book> {

    Optional<Book> findByMetadataSourceAndExternalWorkId(
            String metadataSource,
            String externalWorkId
    );
}
