package com.pecuni.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "test-secret-fixed-value-for-deterministic-tests";

    @Test
    void generatesATokenThatParsesBackToTheSameClaims() {
        JwtService jwtService = new JwtService(new JwtProperties(SECRET, 15, 7));
        UUID userId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        String email = "ana@email.com";

        String token = jwtService.generateAccessToken(userId, workspaceId, email);
        Optional<AuthenticatedUser> parsed = jwtService.parseAndValidate(token);

        assertThat(parsed).isPresent();
        assertThat(parsed.get().userId()).isEqualTo(userId);
        assertThat(parsed.get().workspaceId()).isEqualTo(workspaceId);
        assertThat(parsed.get().email()).isEqualTo(email);
    }

    @Test
    void rejectsAnExpiredToken() {
        JwtService jwtService = new JwtService(new JwtProperties(SECRET, -1, 7));

        String token = jwtService.generateAccessToken(UUID.randomUUID(), UUID.randomUUID(), "ana@email.com");

        assertThat(jwtService.parseAndValidate(token)).isEmpty();
    }

    @Test
    void rejectsATokenSignedWithADifferentSecret() {
        JwtService issuer = new JwtService(new JwtProperties(SECRET, 15, 7));
        JwtService verifier = new JwtService(new JwtProperties("a-completely-different-secret-value-for-this-test", 15, 7));

        String token = issuer.generateAccessToken(UUID.randomUUID(), UUID.randomUUID(), "ana@email.com");

        assertThat(verifier.parseAndValidate(token)).isEmpty();
    }

    @Test
    void rejectsAGarbageToken() {
        JwtService jwtService = new JwtService(new JwtProperties(SECRET, 15, 7));

        assertThat(jwtService.parseAndValidate("not-a-jwt")).isEmpty();
    }

    @Test
    void exposesTheAccessTokenTtlInSeconds() {
        JwtService jwtService = new JwtService(new JwtProperties(SECRET, 15, 7));

        assertThat(jwtService.getAccessTokenTtlSeconds()).isEqualTo(900);
    }
}
