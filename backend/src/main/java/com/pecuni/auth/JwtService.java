package com.pecuni.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private static final String CLAIM_WORKSPACE_ID = "workspaceId";
    private static final String CLAIM_EMAIL = "email";

    private final JwtProperties jwtProperties;
    private final SecretKey key;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.key = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(UUID userId, UUID workspaceId, String email) {
        Instant now = Instant.now();
        Instant expiry = now.plus(jwtProperties.accessTokenTtlMinutes(), ChronoUnit.MINUTES);
        return Jwts.builder()
                .subject(userId.toString())
                .claim(CLAIM_WORKSPACE_ID, workspaceId.toString())
                .claim(CLAIM_EMAIL, email)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key)
                .compact();
    }

    public long getAccessTokenTtlSeconds() {
        return jwtProperties.accessTokenTtlMinutes() * 60;
    }

    public Optional<AuthenticatedUser> parseAndValidate(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            UUID userId = UUID.fromString(claims.getSubject());
            UUID workspaceId = UUID.fromString(claims.get(CLAIM_WORKSPACE_ID, String.class));
            String email = claims.get(CLAIM_EMAIL, String.class);
            return Optional.of(new AuthenticatedUser(userId, workspaceId, email));
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
