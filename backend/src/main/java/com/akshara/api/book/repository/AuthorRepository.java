package com.akshara.api.book.repository;

import com.akshara.api.book.entity.Author;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthorRepository extends JpaRepository<Author, Long> {

    Optional<Author> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);
}