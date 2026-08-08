package com.nexvia.config;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(
                "test-secret-key-que-tiene-al-menos-256-bits-de-largo-para-hmac-sha256",
                3600000L
        );
    }

    @Test
    void generateToken_andExtractUserId() {
        String token = jwtService.generateToken(42L, "user@mail.com", "USUARIO");

        Long userId = jwtService.extractUserId(token);

        assertThat(userId).isEqualTo(42L);
    }

    @Test
    void generateToken_andExtractRole() {
        String token = jwtService.generateToken(1L, "user@mail.com", "CHOFER");

        String role = jwtService.extractRole(token);

        assertThat(role).isEqualTo("CHOFER");
    }

    @Test
    void parseToken_containsEmailClaim() {
        String token = jwtService.generateToken(1L, "test@mail.com", "ADMIN");

        Claims claims = jwtService.parseToken(token);

        assertThat(claims.get("email", String.class)).isEqualTo("test@mail.com");
    }

    @Test
    void isTokenValid_validToken_returnsTrue() {
        String token = jwtService.generateToken(1L, "user@mail.com", "USUARIO");

        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    void isTokenValid_invalidToken_returnsFalse() {
        assertThat(jwtService.isTokenValid("invalid.token.here")).isFalse();
    }

    @Test
    void isTokenValid_tamperedToken_returnsFalse() {
        String token = jwtService.generateToken(1L, "user@mail.com", "USUARIO");
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";

        assertThat(jwtService.isTokenValid(tampered)).isFalse();
    }

    @Test
    void getExpirationMs_returnsConfiguredValue() {
        assertThat(jwtService.getExpirationMs()).isEqualTo(3600000L);
    }
}
