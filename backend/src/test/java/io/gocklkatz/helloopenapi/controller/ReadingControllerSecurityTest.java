package io.gocklkatz.helloopenapi.controller;

import io.gocklkatz.helloopenapi.auth.JwtService;
import io.gocklkatz.helloopenapi.config.SecurityConfig;
import io.gocklkatz.helloopenapi.config.UserConfig;
import io.gocklkatz.helloopenapi.service.ReadingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReadingController.class)
@Import(SecurityConfig.class)
class ReadingControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReadingService readingService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserConfig userConfig;

    @BeforeEach
    void setUp() {
        when(userConfig.getUsers()).thenReturn(List.of());
    }

    @Test
    void getAllReadings_noToken_returns401() throws Exception {
        mockMvc.perform(get("/readings"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getReadingById_noToken_returns401() throws Exception {
        mockMvc.perform(get("/readings/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createReading_noToken_returns401() throws Exception {
        MockMultipartFile image = new MockMultipartFile("image", "meter.jpg", "image/jpeg", "content".getBytes());
        mockMvc.perform(multipart("/readings")
                        .file(image)
                        .param("timestamp", "2026-02-19T08:00:00Z"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAllReadings_withValidBearerToken_returns200() throws Exception {
        when(jwtService.isTokenValid("tok")).thenReturn(true);
        when(jwtService.extractUsername("tok")).thenReturn("user");
        when(readingService.getAllReadings()).thenReturn(List.of());

        mockMvc.perform(get("/readings")
                        .header("Authorization", "Bearer tok"))
                .andExpect(status().isOk());
    }
}
