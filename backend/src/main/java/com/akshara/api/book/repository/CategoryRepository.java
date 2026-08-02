package com.akshara.api.book.repository;

import com.akshara.api.book.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByNameIgnoreCase(String name);

    Optional<Category> findBySlugIgnoreCase(String slug);

    boolean existsByNameIgnoreCase(String name);

    boolean existsBySlugIgnoreCase(String slug);
}