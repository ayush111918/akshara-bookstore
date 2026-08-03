package com.akshara.api.reading.repository;

import com.akshara.api.reading.entity.ReadingAnnotation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReadingAnnotationRepository extends JpaRepository<ReadingAnnotation, Long> {
    List<ReadingAnnotation> findAllByReadingEntry_IdOrderByCreatedAtDesc(Long readingEntryId);
    Optional<ReadingAnnotation> findByIdAndReadingEntry_User_Id(Long id, Long userId);
    long countByReadingEntry_Id(Long readingEntryId);
    void deleteAllByReadingEntry_Id(Long readingEntryId);
}
