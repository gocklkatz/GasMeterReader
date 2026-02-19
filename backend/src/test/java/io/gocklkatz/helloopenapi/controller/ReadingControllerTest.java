package io.gocklkatz.helloopenapi.controller;

import com.example.model.Reading;
import io.gocklkatz.helloopenapi.auth.JwtService;
import io.gocklkatz.helloopenapi.service.ReadingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReadingController.class)
@WithMockUser
class ReadingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReadingService readingService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void createReading_validInput_returns201WithReading() throws Exception {
        OffsetDateTime timestamp = OffsetDateTime.parse("2026-02-19T08:00:00Z");
        Reading reading = new Reading(1, timestamp, "2026/02/19/reading_abc.jpg");
        when(readingService.createReading(any(), any())).thenReturn(reading);

        MockMultipartFile image = new MockMultipartFile("image", "meter.jpg", "image/jpeg", "fake content".getBytes());

        mockMvc.perform(multipart("/readings")
                        .file(image)
                        .param("timestamp", "2026-02-19T08:00:00Z"))
                .andExpect(status().isCreated())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.imagePath").value("2026/02/19/reading_abc.jpg"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(readingService).createReading(any(), any());
    }

    @Test
    void getAllReadings_noReadings_returns200WithEmptyArray() throws Exception {
        when(readingService.getAllReadings()).thenReturn(List.of());

        mockMvc.perform(get("/readings"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(readingService).getAllReadings();
    }

    @Test
    void getAllReadings_withReadings_returns200WithList() throws Exception {
        OffsetDateTime ts1 = OffsetDateTime.parse("2026-02-17T08:00:00Z");
        OffsetDateTime ts2 = OffsetDateTime.parse("2026-02-18T08:00:00Z");
        List<Reading> readings = List.of(
                new Reading(1, ts1, "2026/02/17/reading_a.jpg"),
                new Reading(2, ts2, "2026/02/18/reading_b.jpg")
        );
        when(readingService.getAllReadings()).thenReturn(readings);

        mockMvc.perform(get("/readings"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].imagePath").value("2026/02/17/reading_a.jpg"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].imagePath").value("2026/02/18/reading_b.jpg"));

        verify(readingService).getAllReadings();
    }

    @Test
    void getReadingById_existingId_returns200WithReading() throws Exception {
        OffsetDateTime timestamp = OffsetDateTime.parse("2026-02-19T08:00:00Z");
        Reading reading = new Reading(1, timestamp, "2026/02/19/reading_abc.jpg");
        when(readingService.getReadingById(1)).thenReturn(Optional.of(reading));

        mockMvc.perform(get("/readings/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.imagePath").value("2026/02/19/reading_abc.jpg"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(readingService).getReadingById(1);
    }

    @Test
    void getReadingById_unknownId_returns404() throws Exception {
        when(readingService.getReadingById(99)).thenReturn(Optional.empty());

        mockMvc.perform(get("/readings/99"))
                .andExpect(status().isNotFound());

        verify(readingService).getReadingById(99);
    }

    @Test
    void getReadingById_nonNumericId_returns400() throws Exception {
        mockMvc.perform(get("/readings/abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createReading_missingImage_returns400() throws Exception {
        mockMvc.perform(multipart("/readings")
                        .param("timestamp", "2026-02-19T08:00:00Z"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createReading_missingTimestamp_returns400() throws Exception {
        MockMultipartFile image = new MockMultipartFile("image", "meter.jpg", "image/jpeg", "fake content".getBytes());

        mockMvc.perform(multipart("/readings")
                        .file(image))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createReading_invalidTimestampFormat_returns400() throws Exception {
        MockMultipartFile image = new MockMultipartFile("image", "meter.jpg", "image/jpeg", "fake content".getBytes());

        mockMvc.perform(multipart("/readings")
                        .file(image)
                        .param("timestamp", "not-a-date"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createReading_serviceThrowsException_propagatesOutOfController() {
        MockMultipartFile image = new MockMultipartFile("image", "meter.jpg", "image/jpeg", "fake content".getBytes());
        when(readingService.createReading(any(), any())).thenThrow(new RuntimeException("Storage failure"));

        assertThatThrownBy(() -> mockMvc.perform(multipart("/readings")
                        .file(image)
                        .param("timestamp", "2026-02-19T08:00:00Z")))
                .hasRootCauseInstanceOf(RuntimeException.class)
                .hasRootCauseMessage("Storage failure");
    }
}
