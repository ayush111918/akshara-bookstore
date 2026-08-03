package com.akshara.api.library.service;

import com.akshara.api.library.event.PersonalBookFileDeletionRequested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class PersonalBookFileDeletionListenerTest {

    @TempDir Path storageDirectory;

    @Test
    void removesOnlyACommittedFileInsideTheConfiguredStorageRoot() throws Exception {
        Path storedFile = Files.writeString(storageDirectory.resolve("stored.pdf"), "%PDF-");
        PersonalBookFileDeletionListener listener =
                new PersonalBookFileDeletionListener(storageDirectory.toString());

        listener.deleteCommittedFile(new PersonalBookFileDeletionRequested("stored.pdf"));

        assertThat(storedFile).doesNotExist();
    }

    @Test
    void rejectsTraversalOutsideTheStorageRoot() throws Exception {
        Path outside = Files.writeString(storageDirectory.getParent().resolve("outside.pdf"), "%PDF-");
        PersonalBookFileDeletionListener listener =
                new PersonalBookFileDeletionListener(storageDirectory.toString());

        listener.deleteCommittedFile(new PersonalBookFileDeletionRequested("../outside.pdf"));

        assertThat(outside).exists();
        Files.deleteIfExists(outside);
    }
}
