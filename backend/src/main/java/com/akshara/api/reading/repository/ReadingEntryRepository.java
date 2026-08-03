package com.akshara.api.reading.repository;

import com.akshara.api.reading.entity.ReadingEntry;
import com.akshara.api.reading.entity.ReadingSourceType;
import com.akshara.api.reading.entity.ReadingStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReadingEntryRepository extends JpaRepository<ReadingEntry, Long> {

    @EntityGraph(attributePaths = "user")
    List<ReadingEntry> findAllByUser_IdOrderByUpdatedAtDesc(Long userId);

    Optional<ReadingEntry> findByIdAndUser_Id(Long id, Long userId);

    Optional<ReadingEntry> findByUser_IdAndSourceTypeAndSourceReferenceId(
            Long userId,
            ReadingSourceType sourceType,
            Long sourceReferenceId
    );

    long countByUser_IdAndStatus(Long userId, ReadingStatus status);

    long countByUser_IdAndStatusAndCompletedOnBetween(
            Long userId,
            ReadingStatus status,
            LocalDate start,
            LocalDate end
    );
}
