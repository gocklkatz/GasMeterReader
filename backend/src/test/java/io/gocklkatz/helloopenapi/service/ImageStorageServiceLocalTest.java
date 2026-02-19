package io.gocklkatz.helloopenapi.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ImageStorageServiceLocalTest {

    @TempDir
    Path tempDir;

    private ImageStorageServiceLocal service;

    private static final OffsetDateTime TIMESTAMP = OffsetDateTime.parse("2026-02-19T08:00:00Z");

    @BeforeEach
    void setUp() {
        service = new ImageStorageServiceLocal(tempDir.toString());
    }

    @Test
    void store_createsDateBasedDirectory() {
        MockMultipartFile image = new MockMultipartFile("image", "meter.jpg", "image/jpeg", "content".getBytes());

        service.store(image, TIMESTAMP);

        assertThat(tempDir.resolve("2026/02/19")).isDirectory();
    }

    @Test
    void store_writesFileToCorrectLocation() {
        MockMultipartFile image = new MockMultipartFile("image", "meter.jpg", "image/jpeg", "content".getBytes());

        String relativePath = service.store(image, TIMESTAMP);

        assertThat(tempDir.resolve(relativePath)).exists();
    }

    @Test
    void store_returnsRelativePathWithDatePrefix() {
        MockMultipartFile image = new MockMultipartFile("image", "meter.jpg", "image/jpeg", "content".getBytes());

        String path = service.store(image, TIMESTAMP);

        assertThat(path).startsWith("2026/02/19/reading_");
        assertThat(path).endsWith(".jpg");
    }

    @Test
    void store_preservesOriginalFileExtension() {
        MockMultipartFile image = new MockMultipartFile("image", "photo.png", "image/png", "content".getBytes());

        String path = service.store(image, TIMESTAMP);

        assertThat(path).endsWith(".png");
    }

    @Test
    void store_usesFallbackExtensionWhenFilenameHasNoDot() {
        MockMultipartFile image = new MockMultipartFile("image", "noextension", "image/jpeg", "content".getBytes());

        String path = service.store(image, TIMESTAMP);

        assertThat(path).endsWith(".jpg");
    }

    @Test
    void store_usesFallbackExtensionWhenFilenameIsNull() {
        MockMultipartFile image = new MockMultipartFile("image", null, "image/jpeg", "content".getBytes());

        String path = service.store(image, TIMESTAMP);

        assertThat(path).endsWith(".jpg");
    }

    @Test
    void store_generatesUniqueFilenamesForMultipleCalls() {
        MockMultipartFile image = new MockMultipartFile("image", "meter.jpg", "image/jpeg", "content".getBytes());

        String path1 = service.store(image, TIMESTAMP);
        String path2 = service.store(image, TIMESTAMP);

        assertThat(path1).isNotEqualTo(path2);
    }

    @Test
    void store_usesPaddedMonthAndDay() {
        MockMultipartFile image = new MockMultipartFile("image", "meter.jpg", "image/jpeg", "content".getBytes());
        OffsetDateTime timestampWithSingleDigits = OffsetDateTime.parse("2026-01-05T08:00:00Z");

        String path = service.store(image, timestampWithSingleDigits);

        assertThat(path).startsWith("2026/01/05/");
        assertThat(tempDir.resolve("2026/01/05")).isDirectory();
    }

    @Test
    void clearStorage_deletesExistingFilesAndSubdirectories() throws IOException {
        Path dir = tempDir.resolve("2026/02/19");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("reading_abc.jpg"), "content");

        service.clearStorage();

        assertThat(tempDir.resolve("2026")).doesNotExist();
    }

    @Test
    void clearStorage_baseDirectoryExistsAfterClear() throws IOException {
        service.clearStorage();

        assertThat(tempDir).isDirectory();
    }

    @Test
    void clearStorage_whenBasePathDoesNotExist_createsIt() throws IOException {
        Path nonExistent = tempDir.resolve("new-storage");
        ImageStorageServiceLocal newService = new ImageStorageServiceLocal(nonExistent.toString());

        newService.clearStorage();

        assertThat(nonExistent).isDirectory();
    }

    @Test
    void clearStorage_storeWorksNormallyAfterClear() {
        MockMultipartFile image = new MockMultipartFile("image", "meter.jpg", "image/jpeg", "content".getBytes());

        service.clearStorage();
        String path = service.store(image, TIMESTAMP);

        assertThat(tempDir.resolve(path)).exists();
    }
}
