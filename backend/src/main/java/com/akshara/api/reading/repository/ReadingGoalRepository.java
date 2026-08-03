package com.akshara.api.reading.repository;

import com.akshara.api.reading.entity.ReadingGoal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReadingGoalRepository extends JpaRepository<ReadingGoal, Long> {
    Optional<ReadingGoal> findByUser_IdAndYear(Long userId, Integer year);
    void deleteAllByUser_Id(Long userId);
}
