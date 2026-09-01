package com.pecuni.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pecuni.user.User;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private RefreshTokenService refreshTokenService;
    private User user;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService(refreshTokenRepository, new JwtProperties("secret", 15, 7));
        user = new User();
        user.setEmail("ana@email.com");
    }

    @Test
    void issueStoresOnlyTheHashNeverTheRawToken() {
        String rawToken = refreshTokenService.issue(user);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        RefreshToken saved = captor.getValue();

        assertThat(saved.getTokenHash()).isNotEqualTo(rawToken);
        assertThat(saved.getTokenHash()).hasSize(64); // SHA-256 hex digest
        assertThat(saved.isRevoked()).isFalse();
        assertThat(saved.getUser()).isSameAs(user);
    }

    @Test
    void rotateRevokesTheOldTokenAndIssuesANewOne() {
        RefreshToken stored = new RefreshToken();
        stored.setUser(user);
        stored.setRevoked(false);
        stored.setExpiresAt(Instant.now().plusSeconds(60));
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(stored));

        RefreshTokenService.RotationResult result = refreshTokenService.rotate("some-raw-token");

        assertThat(stored.isRevoked()).isTrue();
        assertThat(result.user()).isSameAs(user);
        assertThat(result.rawRefreshToken()).isNotBlank();
        verify(refreshTokenRepository, times(2)).save(any()); // rotate() delegates the new one to issue()
    }

    @Test
    void rotateRejectsAnExpiredToken() {
        RefreshToken stored = new RefreshToken();
        stored.setUser(user);
        stored.setRevoked(false);
        stored.setExpiresAt(Instant.now().minusSeconds(1));
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> refreshTokenService.rotate("expired-token"))
                .isInstanceOf(BadCredentialsException.class);
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void rotateRejectsAnUnknownToken() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.rotate("unknown-token"))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void reusingARevokedTokenRevokesEveryActiveTokenForThatUserAsContainment() {
        UUID userId = UUID.randomUUID();
        user.setId(userId);
        RefreshToken stored = new RefreshToken();
        stored.setUser(user);
        stored.setRevoked(true);
        stored.setExpiresAt(Instant.now().plusSeconds(60));
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> refreshTokenService.rotate("stolen-token"))
                .isInstanceOf(BadCredentialsException.class);

        verify(refreshTokenRepository).revokeAllActiveForUser(userId);
        verify(refreshTokenRepository, never()).save(any());
    }
}
