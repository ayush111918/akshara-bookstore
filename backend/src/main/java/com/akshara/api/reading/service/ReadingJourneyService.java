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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class ReadingJourneyService {

    private static final int DEFAULT_YEARLY_GOAL = 12;

    private final UserService userService;
    private final OrderItemRepository orderItemRepository;
    private final PersonalBookRepository personalBookRepository;
    private final BookAuthorRepository bookAuthorRepository;
    private final ReadingEntryRepository entryRepository;
    private final ReadingActivityRepository activityRepository;
    private final ReadingAnnotationRepository annotationRepository;
    private final ReadingGoalRepository goalRepository;

    public ReadingJourneyService(
            UserService userService,
            OrderItemRepository orderItemRepository,
            PersonalBookRepository personalBookRepository,
            BookAuthorRepository bookAuthorRepository,
            ReadingEntryRepository entryRepository,
            ReadingActivityRepository activityRepository,
            ReadingAnnotationRepository annotationRepository,
            ReadingGoalRepository goalRepository
    ) {
        this.userService = userService;
        this.orderItemRepository = orderItemRepository;
        this.personalBookRepository = personalBookRepository;
        this.bookAuthorRepository = bookAuthorRepository;
        this.entryRepository = entryRepository;
        this.activityRepository = activityRepository;
        this.annotationRepository = annotationRepository;
        this.goalRepository = goalRepository;
    }

    @Transactional(readOnly = true)
    public ReadingDashboardResponse getDashboard(String subject) {
        AppUser user = userService.getCurrentUserEntity(subject);
        LocalDate today = LocalDate.now();
        int year = today.getYear();
        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate yearEnd = LocalDate.of(year, 12, 31);

        List<ReadingEntryResponse> entries = entryRepository
                .findAllByUser_IdOrderByUpdatedAtDesc(user.getId())
                .stream().map(this::toResponse).toList();
        long currentlyReading = entryRepository.countByUser_IdAndStatus(
                user.getId(), ReadingStatus.READING
        );
        long completed = entryRepository.countByUser_IdAndStatus(
                user.getId(), ReadingStatus.COMPLETED
        );
        long completedThisYear = entryRepository
                .countByUser_IdAndStatusAndCompletedOnBetween(
                        user.getId(), ReadingStatus.COMPLETED, yearStart, yearEnd
                );
        List<LocalDate> activityDates = activityRepository
                .findDistinctActivityDates(user.getId());
        long pagesRead = activityRepository.sumPagesReadBetween(
                user.getId(), yearStart, yearEnd
        );

        ReadingStatisticsResponse statistics = new ReadingStatisticsResponse(
                entries.size(), currentlyReading, completed, completedThisYear,
                pagesRead,
                (int) activityDates.stream().filter(date -> date.getYear() == year).count(),
                currentStreak(activityDates, today), longestStreak(activityDates)
        );
        int target = goalRepository.findByUser_IdAndYear(user.getId(), year)
                .map(ReadingGoal::getTargetBooks)
                .orElse(DEFAULT_YEARLY_GOAL);
        ReadingGoalResponse goal = goalResponse(year, target, completedThisYear);
        return new ReadingDashboardResponse(statistics, goal, entries);
    }

    @Transactional
    public ReadingEntryResponse create(String subject, CreateReadingEntryRequest request) {
        AppUser user = userService.getCurrentUserEntity(subject);
        return entryRepository.findByUser_IdAndSourceTypeAndSourceReferenceId(
                        user.getId(), request.sourceType(), request.sourceId()
                )
                .map(this::toResponse)
                .orElseGet(() -> toResponse(entryRepository.save(
                        buildEntry(user, request)
                )));
    }

    @Transactional
    public ReadingEntryResponse updateProgress(
            String subject,
            Long entryId,
            UpdateReadingProgressRequest request
    ) {
        AppUser user = userService.getCurrentUserEntity(subject);
        ReadingEntry entry = getOwnedEntry(entryId, user.getId());
        Integer totalPages = request.totalPages() == null
                ? entry.getTotalPages() : request.totalPages();
        int nextPage = request.currentPage();
        if (request.status() == ReadingStatus.NOT_STARTED && nextPage != 0) {
            throw new InvalidRequestException("A not-started book must remain on page 0");
        }
        if (totalPages != null && nextPage > totalPages) {
            throw new InvalidRequestException("Current page cannot exceed total pages");
        }
        if (request.status() == ReadingStatus.COMPLETED && totalPages != null) {
            nextPage = totalPages;
        }

        int previousPage = entry.getCurrentPage();
        ReadingStatus previousStatus = entry.getStatus();
        LocalDate today = LocalDate.now();
        entry.updateProgress(request.status(), nextPage, totalPages, today);
        ReadingEntry saved = entryRepository.save(entry);

        if (request.status() != ReadingStatus.NOT_STARTED
                && (nextPage != previousPage || request.status() != previousStatus)) {
            recordActivity(saved, user, today, Math.max(0, nextPage - previousPage));
        }
        return toResponse(saved);
    }

    @Transactional
    public void deleteEntry(String subject, Long entryId) {
        AppUser user = userService.getCurrentUserEntity(subject);
        ReadingEntry entry = getOwnedEntry(entryId, user.getId());
        annotationRepository.deleteAllByReadingEntry_Id(entryId);
        activityRepository.deleteAllByReadingEntry_Id(entryId);
        entryRepository.delete(entry);
    }

    @Transactional(readOnly = true)
    public List<ReadingAnnotationResponse> getAnnotations(String subject, Long entryId) {
        AppUser user = userService.getCurrentUserEntity(subject);
        getOwnedEntry(entryId, user.getId());
        return annotationRepository.findAllByReadingEntry_IdOrderByCreatedAtDesc(entryId)
                .stream().map(this::toAnnotationResponse).toList();
    }

    @Transactional
    public ReadingAnnotationResponse createAnnotation(
            String subject,
            Long entryId,
            ReadingAnnotationRequest request
    ) {
        AppUser user = userService.getCurrentUserEntity(subject);
        ReadingEntry entry = getOwnedEntry(entryId, user.getId());
        validateAnnotationPage(entry, request.pageNumber());
        ReadingAnnotation annotation = new ReadingAnnotation(
                entry, request.type(), request.content().trim(), request.pageNumber()
        );
        return toAnnotationResponse(annotationRepository.save(annotation));
    }

    @Transactional
    public ReadingAnnotationResponse updateAnnotation(
            String subject,
            Long annotationId,
            ReadingAnnotationRequest request
    ) {
        AppUser user = userService.getCurrentUserEntity(subject);
        ReadingAnnotation annotation = annotationRepository
                .findByIdAndReadingEntry_User_Id(annotationId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Reading annotation with ID " + annotationId + " was not found"
                ));
        validateAnnotationPage(annotation.getReadingEntry(), request.pageNumber());
        annotation.update(
                request.type(), request.content().trim(), request.pageNumber()
        );
        return toAnnotationResponse(annotationRepository.save(annotation));
    }

    @Transactional
    public void deleteAnnotation(String subject, Long annotationId) {
        AppUser user = userService.getCurrentUserEntity(subject);
        ReadingAnnotation annotation = annotationRepository
                .findByIdAndReadingEntry_User_Id(annotationId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Reading annotation with ID " + annotationId + " was not found"
                ));
        annotationRepository.delete(annotation);
    }

    @Transactional
    public ReadingGoalResponse updateGoal(
            String subject,
            int year,
            ReadingGoalRequest request
    ) {
        AppUser user = userService.getCurrentUserEntity(subject);
        ReadingGoal goal = goalRepository.findByUser_IdAndYear(user.getId(), year)
                .orElseGet(() -> new ReadingGoal(user, year, request.targetBooks()));
        goal.setTargetBooks(request.targetBooks());
        goalRepository.save(goal);
        long completed = entryRepository.countByUser_IdAndStatusAndCompletedOnBetween(
                user.getId(), ReadingStatus.COMPLETED,
                LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31)
        );
        return goalResponse(year, goal.getTargetBooks(), completed);
    }

    private ReadingEntry buildEntry(AppUser user, CreateReadingEntryRequest request) {
        if (request.sourceType() == ReadingSourceType.PERSONAL_UPLOAD) {
            PersonalBook book = personalBookRepository
                    .findByIdAndUser_Id(request.sourceId(), user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Personal book with ID " + request.sourceId() + " was not found"
                    ));
            return new ReadingEntry(
                    user, request.sourceType(), book.getId(), null,
                    book.getTitle(), book.getAuthor(), null, request.totalPages()
            );
        }

        OrderItem item = orderItemRepository
                .findFirstByOrder_User_IdAndOrder_StatusAndBookEdition_Book_IdOrderByOrder_PlacedAtDescIdDesc(
                        user.getId(), OrderStatus.DELIVERED, request.sourceId()
                )
                .orElseThrow(() -> new InvalidRequestException(
                        "Only a delivered book in My Books can be added to your reading journey"
                ));
        Integer pageCount = request.totalPages() != null
                ? request.totalPages()
                : item.getBookEdition().getPageCount();
        String authors = bookAuthorRepository.findAllByBook_Id(request.sourceId())
                .stream()
                .map(link -> link.getAuthor().getName())
                .sorted()
                .reduce((left, right) -> left + ", " + right)
                .orElse(null);
        return new ReadingEntry(
                user, request.sourceType(), request.sourceId(), request.sourceId(),
                item.getBookTitle(), authors,
                item.getCoverImageUrl(), pageCount
        );
    }

    private ReadingEntry getOwnedEntry(Long entryId, Long userId) {
        return entryRepository.findByIdAndUser_Id(entryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Reading entry with ID " + entryId + " was not found"
                ));
    }

    private void recordActivity(
            ReadingEntry entry,
            AppUser user,
            LocalDate date,
            int pages
    ) {
        ReadingActivity activity = activityRepository
                .findByReadingEntry_IdAndActivityDate(entry.getId(), date)
                .orElseGet(() -> new ReadingActivity(entry, user, date, 0));
        activity.addPages(pages);
        activityRepository.save(activity);
    }

    private void validateAnnotationPage(ReadingEntry entry, Integer pageNumber) {
        if (pageNumber != null && entry.getTotalPages() != null
                && pageNumber > entry.getTotalPages()) {
            throw new InvalidRequestException("Annotation page cannot exceed total pages");
        }
    }

    private ReadingEntryResponse toResponse(ReadingEntry entry) {
        int percentage = entry.getTotalPages() == null || entry.getTotalPages() == 0
                ? 0
                : Math.min(100, (int) Math.round(
                        entry.getCurrentPage() * 100.0 / entry.getTotalPages()
                ));
        return new ReadingEntryResponse(
                entry.getId(), entry.getSourceType(), entry.getSourceReferenceId(),
                entry.getBookId(), entry.getTitleSnapshot(), entry.getAuthorSnapshot(),
                entry.getCoverImageUrlSnapshot(), entry.getStatus(),
                entry.getCurrentPage(), entry.getTotalPages(), percentage,
                entry.getStartedOn(), entry.getCompletedOn(), entry.getLastActivityOn(),
                annotationRepository.countByReadingEntry_Id(entry.getId()),
                entry.getStatus() == ReadingStatus.COMPLETED && entry.getBookId() != null,
                entry.getUpdatedAt()
        );
    }

    private ReadingAnnotationResponse toAnnotationResponse(ReadingAnnotation annotation) {
        return new ReadingAnnotationResponse(
                annotation.getId(), annotation.getReadingEntry().getId(),
                annotation.getType(), annotation.getContent(), annotation.getPageNumber(),
                annotation.getCreatedAt(), annotation.getUpdatedAt()
        );
    }

    private ReadingGoalResponse goalResponse(int year, int target, long completed) {
        int percentage = Math.min(100, (int) Math.round(completed * 100.0 / target));
        return new ReadingGoalResponse(year, target, completed, percentage);
    }

    private int currentStreak(List<LocalDate> dates, LocalDate today) {
        if (dates.isEmpty()) return 0;
        List<LocalDate> sorted = dates.stream().distinct().sorted(Comparator.reverseOrder()).toList();
        LocalDate first = sorted.get(0);
        if (first.isBefore(today.minusDays(1))) return 0;
        int streak = 0;
        LocalDate expected = first;
        for (LocalDate date : sorted) {
            if (!date.equals(expected)) break;
            streak++;
            expected = expected.minusDays(1);
        }
        return streak;
    }

    private int longestStreak(List<LocalDate> dates) {
        if (dates.isEmpty()) return 0;
        List<LocalDate> sorted = new ArrayList<>(dates.stream().distinct().toList());
        sorted.sort(Comparator.naturalOrder());
        int longest = 1;
        int current = 1;
        for (int index = 1; index < sorted.size(); index++) {
            if (sorted.get(index).equals(sorted.get(index - 1).plusDays(1))) {
                current++;
                longest = Math.max(longest, current);
            } else {
                current = 1;
            }
        }
        return longest;
    }
}
