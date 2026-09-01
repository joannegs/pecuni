package com.pecuni.auth;

import com.pecuni.user.User;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

/**
 * Issuance, validation and rotation of refresh tokens. The transaction
 * boundary lives in {@link AuthService} — these methods just need to run
 * inside one, which they do whenever called from there.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;

    public String issue(User user) {
        String rawToken = generateRawToken();
        RefreshToken entity = new RefreshToken();
        entity.setUser(user);
        entity.setTokenHash(hash(rawToken));
        entity.setExpiresAt(Instant.now().plus(jwtProperties.refreshTokenTtlDays(), ChronoUnit.DAYS));
        entity.setRevoked(false);
        refreshTokenRepository.save(entity);
        return rawToken;
    }

    /**
     * Validates the given raw refresh token, revokes it and issues a new
     * pair — rotation on every use (API contract 1.2). Reuse of an
     * already-revoked token is treated as a possible theft signal: every
     * active refresh token for that user is revoked as containment, and the
     * attempt is logged at WARN.
     */
    public RotationResult rotate(String rawToken) {
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(this::invalidRefreshToken);

        if (stored.isRevoked()) {
            log.warn("Reuse of a revoked refresh token detected for user {} — revoking all active refresh tokens.",
                    stored.getUser().getId());
            refreshTokenRepository.revokeAllActiveForUser(stored.getUser().getId());
            throw invalidRefreshToken();
        }

        if (stored.getExpiresAt().isBefore(Instant.now())) {
            throw invalidRefreshToken();
        }

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);
        User user = stored.getUser();
        String newRawToken = issue(user);
        return new RotationResult(user, newRawToken);
    }

    public void revoke(String rawToken) {
        refreshTokenRepository.findByTokenHash(hash(rawToken)).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    private BadCredentialsException invalidRefreshToken() {
        return new BadCredentialsException("Refresh token inválido, expirado ou revogado.");
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 não disponível na JVM", ex);
        }
    }

    public record RotationResult(User user, String rawRefreshToken) {
    }
}
