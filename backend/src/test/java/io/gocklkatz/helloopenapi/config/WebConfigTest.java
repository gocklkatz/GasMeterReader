package io.gocklkatz.helloopenapi.config;

import io.gocklkatz.helloopenapi.auth.JwtService;
import io.gocklkatz.helloopenapi.controller.ReadingController;
import io.gocklkatz.helloopenapi.service.ReadingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReadingController.class)
@Import(SecurityConfig.class)
class WebConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReadingService readingService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void preflight_fromAllowedOrigin_returnsAccessControlHeaders() throws Exception {
        mockMvc.perform(options("/readings")
                        .header("Origin", "http://localhost:4200")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"))
                .andExpect(header().exists("Access-Control-Allow-Methods"));
    }

    @Test
    void preflight_fromDisallowedOrigin_noAccessControlAllowOriginHeader() throws Exception {
        mockMvc.perform(options("/readings")
                        .header("Origin", "http://evil.com")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    @WithMockUser
    void request_fromAllowedOrigin_includesAccessControlAllowOriginHeader() throws Exception {
        mockMvc.perform(get("/readings")
                        .header("Origin", "http://localhost:4200"))
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));
    }
}
