package com.akshara.api.library.repository;

import com.akshara.api.library.entity.PersonalBook;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PersonalBookRepository extends JpaRepository<PersonalBook, Long> {
    List<PersonalBook> findAllByUser_IdOrderByCreatedAtDesc(Long userId);
    Optional<PersonalBook> findByIdAndUser_Id(Long id, Long userId);
}
