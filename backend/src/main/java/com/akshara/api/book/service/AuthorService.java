package com.akshara.api.book.service;

import com.akshara.api.book.dto.AuthorRequest;
import com.akshara.api.book.dto.AuthorResponse;
import com.akshara.api.book.entity.Author;
import com.akshara.api.book.repository.AuthorRepository;
import com.akshara.api.common.exception.DuplicateResourceException;
import com.akshara.api.common.exception.ResourceNotFoundException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class AuthorService {

    private final AuthorRepository authorRepository;

    public AuthorService(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    @Transactional
    public AuthorResponse create(AuthorRequest request) {
        String name = request.name().trim();

        if (authorRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException(
                    "An author named '" + name + "' already exists"
            );
        }

        Author author = new Author(name);
        author.setBiography(normalizeOptional(request.biography()));

        return toResponse(authorRepository.save(author));
    }

    public List<AuthorResponse> getAll() {
        return authorRepository
                .findAll(Sort.by(Sort.Direction.ASC, "name"))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public AuthorResponse getById(Long id) {
        return toResponse(findEntityById(id));
    }

    @Transactional
    public AuthorResponse update(Long id, AuthorRequest request) {
        Author author = findEntityById(id);
        String name = request.name().trim();

        authorRepository.findByNameIgnoreCase(name)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new DuplicateResourceException(
                            "An author named '" + name + "' already exists"
                    );
                });

        author.setName(name);
        author.setBiography(normalizeOptional(request.biography()));

        return toResponse(authorRepository.save(author));
    }

    @Transactional
    public void delete(Long id) {
        Author author = findEntityById(id);
        authorRepository.delete(author);
    }

    private Author findEntityById(Long id) {
        return authorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Author not found with id: " + id
                ));
    }

    private AuthorResponse toResponse(Author author) {
        return new AuthorResponse(
                author.getId(),
                author.getName(),
                author.getBiography(),
                author.getCreatedAt(),
                author.getUpdatedAt()
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