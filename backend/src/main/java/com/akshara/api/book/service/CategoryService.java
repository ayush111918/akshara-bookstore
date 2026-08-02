package com.akshara.api.book.service;

import com.akshara.api.book.dto.CategoryRequest;
import com.akshara.api.book.dto.CategoryResponse;
import com.akshara.api.book.entity.Category;
import com.akshara.api.book.repository.CategoryRepository;
import com.akshara.api.common.exception.DuplicateResourceException;
import com.akshara.api.common.exception.ResourceNotFoundException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        String name = request.name().trim();
        String slug = request.slug().trim();

        checkDuplicatesForCreate(name, slug);

        Category category = new Category(name, slug);
        category.setDescription(
                normalizeOptional(request.description())
        );

        return toResponse(categoryRepository.save(category));
    }

    public List<CategoryResponse> getAll() {
        return categoryRepository
                .findAll(Sort.by(Sort.Direction.ASC, "name"))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public CategoryResponse getById(Long id) {
        return toResponse(findEntityById(id));
    }

    public CategoryResponse getBySlug(String slug) {
        Category category = categoryRepository
                .findBySlugIgnoreCase(slug.trim())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category not found with slug: " + slug
                ));

        return toResponse(category);
    }

    @Transactional
    public CategoryResponse update(
            Long id,
            CategoryRequest request
    ) {
        Category category = findEntityById(id);
        String name = request.name().trim();
        String slug = request.slug().trim();

        checkDuplicatesForUpdate(id, name, slug);

        category.setName(name);
        category.setSlug(slug);
        category.setDescription(
                normalizeOptional(request.description())
        );

        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public void delete(Long id) {
        Category category = findEntityById(id);
        categoryRepository.delete(category);
    }

    private void checkDuplicatesForCreate(
            String name,
            String slug
    ) {
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException(
                    "A category named '" + name + "' already exists"
            );
        }

        if (categoryRepository.existsBySlugIgnoreCase(slug)) {
            throw new DuplicateResourceException(
                    "A category with slug '" + slug + "' already exists"
            );
        }
    }

    private void checkDuplicatesForUpdate(
            Long id,
            String name,
            String slug
    ) {
        categoryRepository.findByNameIgnoreCase(name)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new DuplicateResourceException(
                            "A category named '" + name + "' already exists"
                    );
                });

        categoryRepository.findBySlugIgnoreCase(slug)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new DuplicateResourceException(
                            "A category with slug '" + slug
                                    + "' already exists"
                    );
                });
    }

    private Category findEntityById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category not found with id: " + id
                ));
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}