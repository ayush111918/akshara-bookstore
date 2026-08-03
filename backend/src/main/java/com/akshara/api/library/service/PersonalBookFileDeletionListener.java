package com.akshara.api.library.service;

import com.akshara.api.library.event.PersonalBookFileDeletionRequested;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class PersonalBookFileDeletionListener {

    private static final Logger log = LoggerFactory.getLogger(PersonalBookFileDeletionListener.class);
    private final Path storageRoot;

    public PersonalBookFileDeletionListener(
            @Value("${app.personal-library.storage-path:./data/personal-library}") String storagePath
    ) {
        this.storageRoot = Path.of(storagePath).toAbsolutePath().normalize();
    }

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true
    )
    public void deleteCommittedFile(PersonalBookFileDeletionRequested event) {
        Path path = storageRoot.resolve(event.storedFilename()).normalize();
        if (!path.startsWith(storageRoot)) {
            log.error("Rejected unsafe personal-library deletion path: {}", event.storedFilename());
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            log.error("Could not delete committed personal-library file {}", path, exception);
        }
    }
}
