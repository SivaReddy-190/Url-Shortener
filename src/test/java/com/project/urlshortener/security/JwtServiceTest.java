package com.project.urlshortener.security;

import com.project.urlshortener.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtService Unit Tests")
class JwtServiceTest {

    private JwtService jwtService;
    private User testUser;

    // A valid Base64-encoded 256-bit secret (32 bytes)
    private static final String TEST_SECRET =
            "dGVzdC1zZWNyZXQta2V5LXRoYXQtaXMtbG9uZy1lbm91Z2gtZm9yLWhzMjU2";
    private static final long TEST_EXPIRATION_MS = 3_600_000L; // 1 hour

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "expirationMs", TEST_EXPIRATION_MS);

        testUser = User.builder()
                .id(1L)
                .username("jwtuser")
                .email("jwtuser@example.com")
                .password("hashed-pw")
                .build();
    }

    @Test
    @DisplayName("Should generate a non-null, non-empty JWT token")
    void shouldGenerateNonNullToken() {
        String token = jwtService.generateToken(testUser);

        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    @DisplayName("Should extract the correct username from the token")
    void shouldExtractCorrectUsername() {
        String token = jwtService.generateToken(testUser);

        String extractedUsername = jwtService.extractUsername(token);

        assertThat(extractedUsername).isEqualTo("jwtuser");
    }

    @Test
    @DisplayName("Should validate a token successfully for the correct user")
    void shouldReturnTrueForValidToken() {
        String token = jwtService.generateToken(testUser);

        boolean isValid = jwtService.isTokenValid(token, testUser);

        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should return false when token belongs to a different user")
    void shouldReturnFalseForWrongUser() {
        String token = jwtService.generateToken(testUser);

        User differentUser = User.builder()
                .id(2L)
                .username("otheruser")
                .email("other@example.com")
                .password("hashed-pw")
                .build();

        boolean isValid = jwtService.isTokenValid(token, differentUser);

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should accept URL-safe Base64 secret with underscore and hyphen")
    void shouldAcceptUrlSafeBase64SecretWithUnderscore() {
        JwtService service = new JwtService();
        ReflectionTestUtils.setField(service, "secretKey", "wfGQlBlRrHXuu5sJxYcYUnLgP7TF4zTGYLst8WlTtII=");
        ReflectionTestUtils.setField(service, "expirationMs", TEST_EXPIRATION_MS);

        service.validateSecret();
        String token = service.generateToken(testUser);
        assertThat(service.isTokenValid(token, testUser)).isTrue();
    }

    @Test
    @DisplayName("Should accept plain-text secret of at least 32 characters with underscores")
    void shouldAcceptPlainTextSecretOfAtLeast32Chars() {
        JwtService service = new JwtService();
        ReflectionTestUtils.setField(service, "secretKey", "my_very_long_secret_key_for_jwt_auth_1234567890");
        ReflectionTestUtils.setField(service, "expirationMs", TEST_EXPIRATION_MS);

        service.validateSecret();
        String token = service.generateToken(testUser);
        assertThat(service.isTokenValid(token, testUser)).isTrue();
    }
}
