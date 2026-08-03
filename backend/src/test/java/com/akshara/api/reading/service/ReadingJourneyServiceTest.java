package com.akshara.api.reading.service;

import com.akshara.api.common.exception.InvalidRequestException;
import com.akshara.api.common.exception.ResourceNotFoundException;
import com.akshara.api.book.repository.BookAuthorRepository;
import com.akshara.api.library.entity.PersonalBook;
import com.akshara.api.library.repository.PersonalBookRepository;
import com.akshara.api.order.entity.OrderItem;
import com.akshara.api.order.entity.OrderStatus;
import com.akshara.api.order.repository.OrderItemRepository;
import com.akshara.api.reading.dto.*;
import com.akshara.api.reading.entity.*;
import com.akshara.api.reading.repository.*;
import com.akshara.api.user.entity.AppUser;
import com.akshara.api.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReadingJourneyServiceTest {

    @Mock UserService userService;
    @Mock OrderItemRepository orderItemRepository;
    @Mock PersonalBookRepository personalBookRepository;
    @Mock BookAuthorRepository bookAuthorRepository;
    @Mock ReadingEntryRepository entryRepository;
    @Mock ReadingActivityRepository activityRepository;
    @Mock ReadingAnnotationRepository annotationRepository;
    @Mock ReadingGoalRepository goalRepository;
    @Mock AppUser user;
    @Mock PersonalBook personalBook;

    private ReadingJourneyService service;

    @BeforeEach
    void setUp() {
        service = new ReadingJourneyService(
                userService, orderItemRepository, personalBookRepository,
                bookAuthorRepository,
                entryRepository, activityRepository, annotationRepository,
                goalRepository
        );
    }

    @Test
    void createShouldAddOnlyTheAuthenticatedReadersPersonalUpload() {
        authenticate();
        when(entryRepository.findByUser_IdAndSourceTypeAndSourceReferenceId(
                1L, ReadingSourceType.PERSONAL_UPLOAD, 8L
        )).thenReturn(Optional.empty());
        when(personalBookRepository.findByIdAndUser_Id(8L, 1L))
                .thenReturn(Optional.of(personalBook));
        when(personalBook.getId()).thenReturn(8L);
        when(personalBook.getTitle()).thenReturn("My Private Book");
        when(personalBook.getAuthor()).thenReturn("A Reader");
        when(entryRepository.save(any(ReadingEntry.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create("1", new CreateReadingEntryRequest(
                ReadingSourceType.PERSONAL_UPLOAD, 8L, 240
        ));

        assertThat(response.sourceType()).isEqualTo(ReadingSourceType.PERSONAL_UPLOAD);
        assertThat(response.title()).isEqualTo("My Private Book");
        assertThat(response.totalPages()).isEqualTo(240);
        verify(personalBookRepository).findByIdAndUser_Id(8L, 1L);
    }

    @Test
    void createShouldRejectACatalogueBookOutsideMyBooks() {
        authenticate();
        when(entryRepository.findByUser_IdAndSourceTypeAndSourceReferenceId(
                1L, ReadingSourceType.PURCHASED_BOOK, 12L
        )).thenReturn(Optional.empty());
        when(orderItemRepository
                .findFirstByOrder_User_IdAndOrder_StatusAndBookEdition_Book_IdOrderByOrder_PlacedAtDescIdDesc(
                        1L, OrderStatus.DELIVERED, 12L
                )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create("1", new CreateReadingEntryRequest(
                ReadingSourceType.PURCHASED_BOOK, 12L, null
        )))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Only a delivered book in My Books can be added to your reading journey");
    }

    @Test
    void updateProgressShouldCompleteBookAndRecordDailyPages() {
        authenticate();
        ReadingEntry entry = entry(7L, 300);
        when(entryRepository.findByIdAndUser_Id(7L, 1L)).thenReturn(Optional.of(entry));
        when(entryRepository.save(entry)).thenReturn(entry);
        when(activityRepository.findByReadingEntry_IdAndActivityDate(
                eq(7L), any(LocalDate.class)
        )).thenReturn(Optional.empty());
        when(activityRepository.save(any(ReadingActivity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.updateProgress("1", 7L,
                new UpdateReadingProgressRequest(ReadingStatus.COMPLETED, 175, null));

        assertThat(response.status()).isEqualTo(ReadingStatus.COMPLETED);
        assertThat(response.currentPage()).isEqualTo(300);
        assertThat(response.progressPercentage()).isEqualTo(100);
        assertThat(response.completedOn()).isEqualTo(LocalDate.now());

        ArgumentCaptor<ReadingActivity> activity = ArgumentCaptor.forClass(ReadingActivity.class);
        verify(activityRepository).save(activity.capture());
        assertThat(activity.getValue().getPagesRead()).isEqualTo(300);
    }

    @Test
    void updateProgressShouldRejectPageBeyondBookLength() {
        authenticate();
        ReadingEntry entry = entry(7L, 200);
        when(entryRepository.findByIdAndUser_Id(7L, 1L)).thenReturn(Optional.of(entry));

        assertThatThrownBy(() -> service.updateProgress("1", 7L,
                new UpdateReadingProgressRequest(ReadingStatus.READING, 201, null)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Current page cannot exceed total pages");

        verify(entryRepository, never()).save(any());
        verifyNoInteractions(activityRepository);
    }

    @Test
    void updateAnnotationShouldHideAnotherReadersAnnotation() {
        authenticate();
        when(annotationRepository.findByIdAndReadingEntry_User_Id(9L, 1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateAnnotation(
                "1", 9L,
                new ReadingAnnotationRequest(
                        ReadingAnnotationType.NOTE, "Private thought", 10
                )
        ))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Reading annotation with ID 9 was not found");
    }

    @Test
    void dashboardShouldCalculateStreakAndYearlyGoal() {
        authenticate();
        LocalDate today = LocalDate.now();
        when(entryRepository.findAllByUser_IdOrderByUpdatedAtDesc(1L))
                .thenReturn(List.of());
        when(entryRepository.countByUser_IdAndStatus(1L, ReadingStatus.READING))
                .thenReturn(2L);
        when(entryRepository.countByUser_IdAndStatus(1L, ReadingStatus.COMPLETED))
                .thenReturn(5L);
        when(entryRepository.countByUser_IdAndStatusAndCompletedOnBetween(
                eq(1L), eq(ReadingStatus.COMPLETED), any(), any()
        )).thenReturn(4L);
        when(activityRepository.findDistinctActivityDates(1L)).thenReturn(List.of(
                today, today.minusDays(1), today.minusDays(2), today.minusDays(5)
        ));
        when(activityRepository.sumPagesReadBetween(eq(1L), any(), any()))
                .thenReturn(620L);
        ReadingGoal goal = new ReadingGoal(user, today.getYear(), 20);
        when(goalRepository.findByUser_IdAndYear(1L, today.getYear()))
                .thenReturn(Optional.of(goal));

        ReadingDashboardResponse dashboard = service.getDashboard("1");

        assertThat(dashboard.statistics().currentlyReading()).isEqualTo(2);
        assertThat(dashboard.statistics().completedBooks()).isEqualTo(5);
        assertThat(dashboard.statistics().pagesReadThisYear()).isEqualTo(620);
        assertThat(dashboard.statistics().currentStreak()).isEqualTo(3);
        assertThat(dashboard.statistics().longestStreak()).isEqualTo(3);
        assertThat(dashboard.goal().targetBooks()).isEqualTo(20);
        assertThat(dashboard.goal().progressPercentage()).isEqualTo(20);
    }

    @Test
    void deleteEntryShouldRemovePrivateChildrenBeforeEntry() {
        authenticate();
        ReadingEntry entry = entry(7L, 200);
        when(entryRepository.findByIdAndUser_Id(7L, 1L)).thenReturn(Optional.of(entry));

        service.deleteEntry("1", 7L);

        verify(annotationRepository).deleteAllByReadingEntry_Id(7L);
        verify(activityRepository).deleteAllByReadingEntry_Id(7L);
        verify(entryRepository).delete(entry);
    }

    private void authenticate() {
        when(userService.getCurrentUserEntity("1")).thenReturn(user);
        when(user.getId()).thenReturn(1L);
    }

    private ReadingEntry entry(Long id, int totalPages) {
        ReadingEntry entry = new ReadingEntry(
                user, ReadingSourceType.PURCHASED_BOOK, 12L, 12L,
                "A Book", "A Publisher", null, totalPages
        );
        ReflectionTestUtils.setField(entry, "id", id);
        return entry;
    }
}
