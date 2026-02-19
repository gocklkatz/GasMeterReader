package io.gocklkatz.helloopenapi.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class S3ImageStorageServiceLocalTest {

    @Mock
    private S3Client s3Client;

    private ImageStorageServiceS3 service;

    private static final String BUCKET = "gas-meter-bucket";
    private static final OffsetDateTime TIMESTAMP = OffsetDateTime.parse("2026-02-19T08:00:00Z");

    @BeforeEach
    void setUp() {
        service = new ImageStorageServiceS3(s3Client, BUCKET);
    }

    @Test
    void store_uploadsToCorrectBucketAndKey() {
        MockMultipartFile image = new MockMultipartFile("image", "meter.jpg", "image/jpeg", "content".getBytes());

        String key = service.store(image, TIMESTAMP);

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));
        assertThat(requestCaptor.getValue().bucket()).isEqualTo(BUCKET);
        assertThat(requestCaptor.getValue().key()).isEqualTo(key);
    }

    @Test
    void store_returnsKeyWithDatePrefix() {
        MockMultipartFile image = new MockMultipartFile("image", "meter.jpg", "image/jpeg", "content".getBytes());

        String key = service.store(image, TIMESTAMP);

        assertThat(key).startsWith("2026/02/19/reading_");
        assertThat(key).endsWith(".jpg");
    }

    @Test
    void store_setsContentTypeOnRequest() {
        MockMultipartFile image = new MockMultipartFile("image", "meter.jpg", "image/jpeg", "content".getBytes());

        service.store(image, TIMESTAMP);

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));
        assertThat(requestCaptor.getValue().contentType()).isEqualTo("image/jpeg");
    }

    @Test
    void store_preservesOriginalFileExtension() {
        MockMultipartFile image = new MockMultipartFile("image", "photo.png", "image/png", "content".getBytes());

        String key = service.store(image, TIMESTAMP);

        assertThat(key).endsWith(".png");
    }

    @Test
    void store_usesFallbackExtensionWhenFilenameHasNoDot() {
        MockMultipartFile image = new MockMultipartFile("image", "noextension", "image/jpeg", "content".getBytes());

        String key = service.store(image, TIMESTAMP);

        assertThat(key).endsWith(".jpg");
    }

    @Test
    void store_usesFallbackExtensionWhenFilenameIsNull() {
        MockMultipartFile image = new MockMultipartFile("image", null, "image/jpeg", "content".getBytes());

        String key = service.store(image, TIMESTAMP);

        assertThat(key).endsWith(".jpg");
    }

    @Test
    void store_usesPaddedMonthAndDay() {
        MockMultipartFile image = new MockMultipartFile("image", "meter.jpg", "image/jpeg", "content".getBytes());
        OffsetDateTime timestampWithSingleDigits = OffsetDateTime.parse("2026-01-05T08:00:00Z");

        String key = service.store(image, timestampWithSingleDigits);

        assertThat(key).startsWith("2026/01/05/");
    }

    @Test
    void store_generatesUniqueKeysForMultipleCalls() {
        MockMultipartFile image = new MockMultipartFile("image", "meter.jpg", "image/jpeg", "content".getBytes());

        String key1 = service.store(image, TIMESTAMP);
        String key2 = service.store(image, TIMESTAMP);

        assertThat(key1).isNotEqualTo(key2);
    }

    @Test
    void store_sdkException_throwsRuntimeException() {
        MockMultipartFile image = new MockMultipartFile("image", "meter.jpg", "image/jpeg", "content".getBytes());
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(SdkException.create("S3 unavailable", null));

        assertThatThrownBy(() -> service.store(image, TIMESTAMP))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to upload image to S3");
    }

    @Test
    void store_ioException_throwsUncheckedIOException() throws IOException {
        MultipartFile image = org.mockito.Mockito.mock(MultipartFile.class);
        when(image.getOriginalFilename()).thenReturn("meter.jpg");
        when(image.getContentType()).thenReturn("image/jpeg");
        when(image.getBytes()).thenThrow(new IOException("read error"));

        assertThatThrownBy(() -> service.store(image, TIMESTAMP))
                .isInstanceOf(UncheckedIOException.class)
                .hasMessageContaining("Failed to upload image to S3");
    }
}
