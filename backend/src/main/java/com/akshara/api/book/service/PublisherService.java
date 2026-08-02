package com.akshara.api.book.service;

import com.akshara.api.book.dto.PublisherRequest;
import com.akshara.api.book.dto.PublisherResponse;
import com.akshara.api.book.entity.Publisher;
import com.akshara.api.book.repository.PublisherRepository;
import com.akshara.api.common.exception.DuplicateResourceException;
import com.akshara.api.common.exception.ResourceNotFoundException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class PublisherService {

    private final PublisherRepository publisherRepository;

    public PublisherService(PublisherRepository publisherRepository) {
        this.publisherRepository = publisherRepository;
    }

    @Transactional
    public PublisherResponse create(PublisherRequest request) {
        String name = request.name().trim();

        if (publisherRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException(
                    "A publisher named '" + name + "' already exists"
            );
        }

        Publisher publisher = new Publisher(name);
        publisher.setWebsiteUrl(normalizeOptional(request.websiteUrl()));

        return toResponse(publisherRepository.save(publisher));
    }

    public List<PublisherResponse> getAll() {
        return publisherRepository
                .findAll(Sort.by(Sort.Direction.ASC, "name"))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public PublisherResponse getById(Long id) {
        return toResponse(findEntityById(id));
    }

    @Transactional
    public PublisherResponse update(
            Long id,
            PublisherRequest request
    ) {
        Publisher publisher = findEntityById(id);
        String name = request.name().trim();

        publisherRepository.findByNameIgnoreCase(name)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new DuplicateResourceException(
                            "A publisher named '" + name + "' already exists"
                    );
                });

        publisher.setName(name);
        publisher.setWebsiteUrl(
                normalizeOptional(request.websiteUrl())
        );

        return toResponse(publisherRepository.save(publisher));
    }

    @Transactional
    public void delete(Long id) {
        Publisher publisher = findEntityById(id);
        publisherRepository.delete(publisher);
    }

    private Publisher findEntityById(Long id) {
        return publisherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Publisher not found with id: " + id
                ));
    }

    private PublisherResponse toResponse(Publisher publisher) {
        return new PublisherResponse(
                publisher.getId(),
                publisher.getName(),
                publisher.getWebsiteUrl(),
                publisher.getCreatedAt(),
                publisher.getUpdatedAt()
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