package com.akshara.api.library.service;

import com.akshara.api.auth.exception.InvalidAccessTokenException;
import com.akshara.api.book.entity.BookFormat;
import com.akshara.api.common.exception.InvalidRequestException;
import com.akshara.api.common.exception.ResourceNotFoundException;
import com.akshara.api.library.dto.PersonalBookFile;
import com.akshara.api.library.dto.PersonalBookResponse;
import com.akshara.api.library.entity.PersonalBook;
import com.akshara.api.library.exception.PersonalLibraryStorageException;
import com.akshara.api.library.repository.PersonalBookRepository;
import com.akshara.api.user.entity.AppUser;
import com.akshara.api.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.zip.ZipInputStream;

@Service
public class PersonalLibraryService {

    private static final long MAX_FILE_SIZE = 25L * 1024L * 1024L;
    private final UserRepository userRepository;
    private final PersonalBookRepository personalBookRepository;
    private final Path storageRoot;

    public PersonalLibraryService(
            UserRepository userRepository,
            PersonalBookRepository personalBookRepository,
            @Value("${app.personal-library.storage-path:./data/personal-library}") String storagePath
    ) {
        this.userRepository = userRepository;
        this.personalBookRepository = personalBookRepository;
        this.storageRoot = Path.of(storagePath).toAbsolutePath().normalize();
    }

    @Transactional(readOnly = true)
    public List<PersonalBookResponse> getBooks(String subject) {
        AppUser user = getAuthenticatedUser(subject);
        return personalBookRepository.findAllByUser_IdOrderByCreatedAtDesc(user.getId())
                .stream().map(this::toResponse).toList();
    }

    public PersonalBookResponse upload(String subject, String title, String author, MultipartFile file) {
        AppUser user = getAuthenticatedUser(subject);
        String cleanTitle = title == null ? "" : title.trim();
        if (cleanTitle.isBlank() || cleanTitle.length() > 255) {
            throw new InvalidRequestException("Title is required and must not exceed 255 characters");
        }
        String cleanAuthor = author == null || author.isBlank() ? null : author.trim();
        if (cleanAuthor != null && cleanAuthor.length() > 180) {
            throw new InvalidRequestException("Author must not exceed 180 characters");
        }
        if (file == null || file.isEmpty()) {
            throw new InvalidRequestException("Choose a PDF or EPUB file to upload");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new InvalidRequestException("Personal book files must be 25 MB or smaller");
        }

        String originalName = safeOriginalFilename(file.getOriginalFilename());
        BookFormat format = detectAndValidateFormat(originalName, file);
        String extension = format == BookFormat.PDF ? ".pdf" : ".epub";
        String storedName = UUID.randomUUID() + extension;
        Path target = resolveStoredFile(storedName);

        try {
            Files.createDirectories(storageRoot);
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }
            PersonalBook saved = personalBookRepository.save(new PersonalBook(
                    user, cleanTitle, cleanAuthor, format, originalName, storedName,
                    format == BookFormat.PDF ? "application/pdf" : "application/epub+zip",
                    file.getSize()
            ));
            return toResponse(saved);
        } catch (RuntimeException | IOException exception) {
            deleteQuietly(target);
            if (exception instanceof InvalidRequestException invalidRequestException) {
                throw invalidRequestException;
            }
            throw new PersonalLibraryStorageException("The personal book could not be stored", exception);
        }
    }

    @Transactional(readOnly = true)
    public PersonalBookFile getFile(String subject, Long bookId) {
        PersonalBook book = getOwnedBook(subject, bookId);
        Path path = resolveStoredFile(book.getStoredFilename());
        if (!Files.isRegularFile(path)) {
            throw new ResourceNotFoundException("The uploaded file is no longer available");
        }
        return new PersonalBookFile(new FileSystemResource(path), book.getMediaType(), book.getOriginalFilename());
    }

    public void delete(String subject, Long bookId) {
        PersonalBook book = getOwnedBook(subject, bookId);
        personalBookRepository.delete(book);
        deleteQuietly(resolveStoredFile(book.getStoredFilename()));
    }

    @Transactional
    public void deleteAllForUser(Long userId) {
        List<PersonalBook> books = personalBookRepository.findAllByUser_IdOrderByCreatedAtDesc(userId);
        personalBookRepository.deleteAll(books);
        books.forEach(book -> deleteQuietly(resolveStoredFile(book.getStoredFilename())));
    }

    private PersonalBook getOwnedBook(String subject, Long bookId) {
        AppUser user = getAuthenticatedUser(subject);
        return personalBookRepository.findByIdAndUser_Id(bookId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Personal book with ID " + bookId + " was not found"));
    }

    private BookFormat detectAndValidateFormat(String filename, MultipartFile file) {
        String lower = filename.toLowerCase(Locale.ROOT);
        try (InputStream input = file.getInputStream()) {
            byte[] signature = input.readNBytes(5);
            if (lower.endsWith(".pdf") && signature.length >= 5
                    && signature[0] == '%' && signature[1] == 'P' && signature[2] == 'D'
                    && signature[3] == 'F' && signature[4] == '-') {
                return BookFormat.PDF;
            }
            if (lower.endsWith(".epub") && signature.length >= 2
                    && signature[0] == 'P' && signature[1] == 'K'
                    && hasEpubMimeType(file)) {
                return BookFormat.EPUB;
            }
        } catch (IOException exception) {
            throw new PersonalLibraryStorageException("The uploaded file could not be inspected", exception);
        }
        throw new InvalidRequestException("Only valid PDF and EPUB files can be uploaded");
    }

    private boolean hasEpubMimeType(MultipartFile file) throws IOException {
        try (ZipInputStream zip = new ZipInputStream(file.getInputStream())) {
            var firstEntry = zip.getNextEntry();
            if (firstEntry == null || !"mimetype".equals(firstEntry.getName())) return false;
            String mimeType = new String(zip.readNBytes(30), StandardCharsets.US_ASCII).trim();
            return "application/epub+zip".equals(mimeType);
        }
    }

    private String safeOriginalFilename(String filename) {
        if (filename == null || filename.isBlank()) return "personal-book";
        String normalized = filename.replace('\\', '/');
        String safe = normalized.substring(normalized.lastIndexOf('/') + 1)
                .replaceAll("[\\p{Cntrl}]", "_");
        if (safe.isBlank()) safe = "personal-book";
        return safe.length() > 255 ? safe.substring(safe.length() - 255) : safe;
    }

    private Path resolveStoredFile(String storedName) {
        Path resolved = storageRoot.resolve(storedName).normalize();
        if (!resolved.startsWith(storageRoot)) {
            throw new InvalidRequestException("Invalid personal-library file path");
        }
        return resolved;
    }

    private void deleteQuietly(Path path) {
        try { Files.deleteIfExists(path); } catch (IOException ignored) { }
    }

    private PersonalBookResponse toResponse(PersonalBook book) {
        return new PersonalBookResponse(book.getId(), book.getTitle(), book.getAuthor(), book.getFormat(),
                book.getOriginalFilename(), book.getFileSize(), book.getCreatedAt());
    }

    private AppUser getAuthenticatedUser(String subject) {
        try {
            Long userId = Long.parseLong(subject);
            return userRepository.findById(userId).filter(AppUser::isEnabled)
                    .orElseThrow(InvalidAccessTokenException::new);
        } catch (NumberFormatException exception) {
            throw new InvalidAccessTokenException();
        }
    }
}
