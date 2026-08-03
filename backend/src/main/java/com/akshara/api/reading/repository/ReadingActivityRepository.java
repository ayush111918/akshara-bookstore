package com.akshara.api.reading.repository;

import com.akshara.api.reading.entity.ReadingActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReadingActivityRepository extends JpaRepository<ReadingActivity, Long> {

    Optional<ReadingActivity> findByReadingEntry_IdAndActivityDate(
            Long readingEntryId,
            LocalDate activityDate
    );

    @Query("""
            select distinct activity.activityDate
            from ReadingActivity activity
            where activity.user.id = :userId
            order by activity.activityDate desc
            """)
    List<LocalDate> findDistinctActivityDates(@Param("userId") Long userId);

    @Query("""
            select coalesce(sum(activity.pagesRead), 0)
            from ReadingActivity activity
            where activity.user.id = :userId
              and activity.activityDate between :start and :end
            """)
    Long sumPagesReadBetween(
            @Param("userId") Long userId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    void deleteAllByReadingEntry_Id(Long readingEntryId);
}
