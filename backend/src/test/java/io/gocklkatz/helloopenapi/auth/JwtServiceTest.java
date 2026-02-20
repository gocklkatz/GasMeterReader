package io.gocklkatz.helloopenapi.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    // 64 characters — well above the 32-byte minimum for HS256
    private static final String TEST_SECRET =
            "test-jwt-secret-key-for-unit-testing-purposes-must-be-long-enuf";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(TEST_SECRET, 3_600_000L);
    }

    @Test
    void generateToken_producesNonBlankJwt() {
        String token = jwtService.generateToken("testuser");
        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    void extractUsername_returnsCorrectSubject() {
        String token = jwtService.generateToken("testuser");
        assertThat(jwtService.extractUsername(token)).isEqualTo("testuser");
    }

    @Test
    void isTokenValid_withValidToken_returnsTrue() {
        String token = jwtService.generateToken("testuser");
        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    void isTokenValid_withTamperedToken_returnsFalse() {
        String token = jwtService.generateToken("testuser") + "tampered";
        assertThat(jwtService.isTokenValid(token)).isFalse();
    }

    @Test
    void isTokenValid_withExpiredToken_returnsFalse() {
        JwtService expiredService = new JwtService(TEST_SECRET, -1000L);
        String token = expiredService.generateToken("testuser");
        assertThat(expiredService.isTokenValid(token)).isFalse();
    }

    @Test
    void generateToken_differentUsersProduceDifferentTokens() {
        String token1 = jwtService.generateToken("alice");
        String token2 = jwtService.generateToken("bob");
        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    void constructor_withSecretShorterThan32Chars_throwsIllegalStateException() {
        assertThatThrownBy(() -> new JwtService("short", 86400000L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.security.jwt.secret must be at least 32 characters");
    }
}
