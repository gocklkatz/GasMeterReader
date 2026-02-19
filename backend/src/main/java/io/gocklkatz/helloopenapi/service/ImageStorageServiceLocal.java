package io.gocklkatz.helloopenapi.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@ConditionalOnProperty(name = "app.image-storage.backend", havingValue = "local", matchIfMissing = true)
public class ImageStorageServiceLocal implements ImageStorageService {

    private final Path basePath;

    public ImageStorageServiceLocal(@Value("${app.image-storage.base-path:/data/images}") String basePath) {
        this.basePath = Path.of(basePath);
    }

    @PostConstruct
    void clearStorage() {
        try {
            if (Files.exists(basePath)) {
                try (Stream<Path> walk = Files.walk(basePath)) {
                    walk.sorted(Comparator.reverseOrder())
                            .forEach(p -> {
                                try { Files.delete(p); }
                                catch (IOException e) { throw new UncheckedIOException(e); }
                            });
                }
            }
            Files.createDirectories(basePath);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to initialize image storage", e);
        }
    }

    @Override
    public String store(MultipartFile image, OffsetDateTime timestamp) {
        String year = String.format("%04d", timestamp.getYear());
        String month = String.format("%02d", timestamp.getMonthValue());
        String day = String.format("%02d", timestamp.getDayOfMonth());

        Path dayDir = basePath.resolve(year).resolve(month).resolve(day);
        try {
            Files.createDirectories(dayDir);

            String originalFilename = image.getOriginalFilename();
            String ext = (originalFilename != null && originalFilename.contains("."))
                    ? originalFilename.substring(originalFilename.lastIndexOf('.'))
                    : ".jpg";
            String filename = "reading_" + UUID.randomUUID() + ext;

            image.transferTo(dayDir.resolve(filename));

            return year + "/" + month + "/" + day + "/" + filename;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store image", e);
        }
    }
}
