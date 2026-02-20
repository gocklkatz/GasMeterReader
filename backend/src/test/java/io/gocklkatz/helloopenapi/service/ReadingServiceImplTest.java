package io.gocklkatz.helloopenapi.service;

import com.example.model.Reading;
import io.gocklkatz.helloopenapi.repository.ReadingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReadingServiceImplTest {

    @Mock
    private ImageStorageService imageStorageService;

    @Mock
    private ReadingRepository readingRepository;

    @InjectMocks
    private ReadingServiceImpl readingService;

    @Test
    void createReading_validInput_storesImageAndSavesReading() {
        OffsetDateTime timestamp = OffsetDateTime.parse("2026-02-19T08:00:00Z");
        MockMultipartFile image = new MockMultipartFile("image", "meter.jpg", "image/jpeg", "content".getBytes());
        Reading saved = new Reading(1, timestamp, "2026/02/19/reading_abc.jpg");

        when(imageStorageService.store(image, timestamp)).thenReturn("2026/02/19/reading_abc.jpg");
        when(readingRepository.save(any(Reading.class))).thenReturn(saved);

        Reading result = readingService.createReading(image, timestamp);

        assertThat(result.getId()).isEqualTo(1);
        assertThat(result.getImagePath()).isEqualTo("2026/02/19/reading_abc.jpg");
        assertThat(result.getTimestamp()).isEqualTo(timestamp);
        verify(imageStorageService).store(image, timestamp);
        verify(readingRepository).save(any(Reading.class));
    }

    @Test
    void createReading_passesCorrectDataToRepository() {
        OffsetDateTime timestamp = OffsetDateTime.parse("2026-02-19T08:00:00Z");
        MockMultipartFile image = new MockMultipartFile("image", "meter.jpg", "image/jpeg", "content".getBytes());
        Reading saved = new Reading(1, timestamp, "2026/02/19/reading_abc.jpg");

        when(imageStorageService.store(image, timestamp)).thenReturn("2026/02/19/reading_abc.jpg");
        when(readingRepository.save(any(Reading.class))).thenReturn(saved);

        readingService.createReading(image, timestamp);

        ArgumentCaptor<Reading> captor = ArgumentCaptor.forClass(Reading.class);
        verify(readingRepository).save(captor.capture());
        assertThat(captor.getValue().getTimestamp()).isEqualTo(timestamp);
        assertThat(captor.getValue().getImagePath()).isEqualTo("2026/02/19/reading_abc.jpg");
    }

    @Test
    void createReading_unsupportedContentType_throwsIllegalArgumentException() {
        OffsetDateTime timestamp = OffsetDateTime.parse("2026-02-19T08:00:00Z");
        MockMultipartFile pdf = new MockMultipartFile("image", "doc.pdf", "application/pdf", "content".getBytes());

        assertThatThrownBy(() -> readingService.createReading(pdf, timestamp))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("application/pdf");
        verifyNoInteractions(imageStorageService);
        verifyNoInteractions(readingRepository);
    }

    @Test
    void createReading_nullContentType_throwsIllegalArgumentException() {
        OffsetDateTime timestamp = OffsetDateTime.parse("2026-02-19T08:00:00Z");
        MockMultipartFile noType = new MockMultipartFile("image", "file", null, "content".getBytes());

        assertThatThrownBy(() -> readingService.createReading(noType, timestamp))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(imageStorageService);
    }

    @Test
    void createReading_storageFailure_propagatesExceptionWithoutCallingRepository() {
        OffsetDateTime timestamp = OffsetDateTime.parse("2026-02-19T08:00:00Z");
        MockMultipartFile image = new MockMultipartFile("image", "meter.jpg", "image/jpeg", "content".getBytes());

        when(imageStorageService.store(any(), any()))
                .thenThrow(new UncheckedIOException("disk full", new IOException("disk full")));

        assertThatThrownBy(() -> readingService.createReading(image, timestamp))
                .isInstanceOf(UncheckedIOException.class);
        verifyNoInteractions(readingRepository);
    }

    @Test
    void getAllReadings_delegatesToRepository() {
        OffsetDateTime timestamp = OffsetDateTime.parse("2026-02-19T08:00:00Z");
        List<Reading> readings = List.of(new Reading(1, timestamp, "2026/02/19/reading_abc.jpg"));
        when(readingRepository.findAll()).thenReturn(readings);

        List<Reading> result = readingService.getAllReadings();

        assertThat(result).isEqualTo(readings);
        verify(readingRepository).findAll();
    }

    @Test
    void getAllReadings_noReadings_returnsEmptyList() {
        when(readingRepository.findAll()).thenReturn(List.of());

        List<Reading> result = readingService.getAllReadings();

        assertThat(result).isEmpty();
    }

    @Test
    void getReadingById_existingId_returnsReading() {
        OffsetDateTime timestamp = OffsetDateTime.parse("2026-02-19T08:00:00Z");
        Reading reading = new Reading(1, timestamp, "2026/02/19/reading_abc.jpg");
        when(readingRepository.findById(1)).thenReturn(Optional.of(reading));

        Optional<Reading> result = readingService.getReadingById(1);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(reading);
        verify(readingRepository).findById(1);
    }

    @Test
    void getReadingById_unknownId_returnsEmpty() {
        when(readingRepository.findById(99)).thenReturn(Optional.empty());

        Optional<Reading> result = readingService.getReadingById(99);

        assertThat(result).isEmpty();
        verify(readingRepository).findById(99);
    }

    @Test
    void createReading_imagePng_isAccepted() {
        OffsetDateTime timestamp = OffsetDateTime.parse("2026-02-19T08:00:00Z");
        MockMultipartFile image = new MockMultipartFile("image", "meter.png", "image/png", "content".getBytes());
        Reading saved = new Reading(1, timestamp, "2026/02/19/reading_abc.png");

        when(imageStorageService.store(image, timestamp)).thenReturn("2026/02/19/reading_abc.png");
        when(readingRepository.save(any(Reading.class))).thenReturn(saved);

        Reading result = readingService.createReading(image, timestamp);

        assertThat(result.getId()).isEqualTo(1);
        assertThat(result.getImagePath()).isEqualTo("2026/02/19/reading_abc.png");
    }

    @Test
    void createReading_imageWebp_isAccepted() {
        OffsetDateTime timestamp = OffsetDateTime.parse("2026-02-19T08:00:00Z");
        MockMultipartFile image = new MockMultipartFile("image", "meter.webp", "image/webp", "content".getBytes());
        Reading saved = new Reading(1, timestamp, "2026/02/19/reading_abc.webp");

        when(imageStorageService.store(image, timestamp)).thenReturn("2026/02/19/reading_abc.webp");
        when(readingRepository.save(any(Reading.class))).thenReturn(saved);

        Reading result = readingService.createReading(image, timestamp);

        assertThat(result.getId()).isEqualTo(1);
        assertThat(result.getImagePath()).isEqualTo("2026/02/19/reading_abc.webp");
    }

    @Test
    void createReading_imageGif_isAccepted() {
        OffsetDateTime timestamp = OffsetDateTime.parse("2026-02-19T08:00:00Z");
        MockMultipartFile image = new MockMultipartFile("image", "meter.gif", "image/gif", "content".getBytes());
        Reading saved = new Reading(1, timestamp, "2026/02/19/reading_abc.gif");

        when(imageStorageService.store(image, timestamp)).thenReturn("2026/02/19/reading_abc.gif");
        when(readingRepository.save(any(Reading.class))).thenReturn(saved);

        Reading result = readingService.createReading(image, timestamp);

        assertThat(result.getId()).isEqualTo(1);
        assertThat(result.getImagePath()).isEqualTo("2026/02/19/reading_abc.gif");
    }
}
