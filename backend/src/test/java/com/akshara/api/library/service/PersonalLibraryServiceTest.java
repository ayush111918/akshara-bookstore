package com.akshara.api.library.service;

import com.akshara.api.book.entity.BookFormat;
import com.akshara.api.common.exception.InvalidRequestException;
import com.akshara.api.common.exception.ResourceNotFoundException;
import com.akshara.api.library.entity.PersonalBook;
import com.akshara.api.library.repository.PersonalBookRepository;
import com.akshara.api.user.entity.AppUser;
import com.akshara.api.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonalLibraryServiceTest {

    @TempDir
    Path storageDirectory;

    @Mock
    UserRepository userRepository;

    @Mock
    PersonalBookRepository personalBookRepository;

    @Mock
    AppUser user;

    private PersonalLibraryService service;

    @BeforeEach
    void setUp() {
        service = new PersonalLibraryService(userRepository, personalBookRepository, storageDirectory.toString());
    }

    @Test
    void uploadShouldStoreAValidPdfWithGeneratedFilename() throws Exception {
        authenticate();
        when(personalBookRepository.save(any(PersonalBook.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        MockMultipartFile file = new MockMultipartFile(
                "file", "../my-book.pdf", "application/pdf", "%PDF-1.7\ncontent".getBytes()
        );

        var response = service.upload("1", " My Book ", " An Author ", file);

        assertThat(response.title()).isEqualTo("My Book");
        assertThat(response.author()).isEqualTo("An Author");
        assertThat(response.format()).isEqualTo(BookFormat.PDF);
        assertThat(response.originalFilename()).isEqualTo("my-book.pdf");

        ArgumentCaptor<PersonalBook> captor = ArgumentCaptor.forClass(PersonalBook.class);
        verify(personalBookRepository).save(captor.capture());
        assertThat(captor.getValue().getStoredFilename())
                .matches("[0-9a-f-]{36}\\.pdf");
        try (var files = Files.list(storageDirectory)) {
            assertThat(files).hasSize(1);
        }
    }

    @Test
    void uploadShouldRejectAFileWhoseContentsDoNotMatchItsExtension() throws Exception {
        authenticate();
        MockMultipartFile file = new MockMultipartFile(
                "file", "unsafe.pdf", "application/pdf", "not a pdf".getBytes()
        );

        assertThatThrownBy(() -> service.upload("1", "Unsafe", null, file))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Only valid PDF and EPUB files can be uploaded");

        verifyNoInteractions(personalBookRepository);
        try (var files = Files.list(storageDirectory)) {
            assertThat(files).isEmpty();
        }
    }

    @Test
    void getFileShouldNotExposeAnotherUsersUpload() {
        authenticate();
        when(user.getId()).thenReturn(1L);
        when(personalBookRepository.findByIdAndUser_Id(9L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getFile("1", 9L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Personal book with ID 9 was not found");
    }

    private void authenticate() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(user.isEnabled()).thenReturn(true);
    }
}
