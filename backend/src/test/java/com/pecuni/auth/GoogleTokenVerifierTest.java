package com.pecuni.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import java.io.IOException;
import java.security.GeneralSecurityException;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;

class GoogleTokenVerifierTest {

    @Test
    void extractsEmailAndNameFromAValidToken() throws Exception {
        GoogleIdTokenVerifier delegate = mock(GoogleIdTokenVerifier.class);
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload().setEmail("ana@email.com");
        payload.set("name", "Ana Souza");
        GoogleIdToken googleIdToken = mock(GoogleIdToken.class);
        when(googleIdToken.getPayload()).thenReturn(payload);
        when(delegate.verify("valid-token")).thenReturn(googleIdToken);

        GoogleTokenVerifier verifier = new GoogleTokenVerifier(delegate);
        GoogleTokenVerifier.GoogleUserInfo result = verifier.verify("valid-token");

        assertThat(result.email()).isEqualTo("ana@email.com");
        assertThat(result.name()).isEqualTo("Ana Souza");
    }

    @Test
    void fallsBackToEmailWhenNameClaimIsMissing() throws Exception {
        GoogleIdTokenVerifier delegate = mock(GoogleIdTokenVerifier.class);
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload().setEmail("ana@email.com");
        GoogleIdToken googleIdToken = mock(GoogleIdToken.class);
        when(googleIdToken.getPayload()).thenReturn(payload);
        when(delegate.verify("valid-token")).thenReturn(googleIdToken);

        GoogleTokenVerifier verifier = new GoogleTokenVerifier(delegate);
        GoogleTokenVerifier.GoogleUserInfo result = verifier.verify("valid-token");

        assertThat(result.name()).isEqualTo("ana@email.com");
    }

    @Test
    void rejectsATokenGoogleFailsToVerify() throws Exception {
        GoogleIdTokenVerifier delegate = mock(GoogleIdTokenVerifier.class);
        when(delegate.verify("invalid-token")).thenReturn(null);

        GoogleTokenVerifier verifier = new GoogleTokenVerifier(delegate);

        assertThatThrownBy(() -> verifier.verify("invalid-token")).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void rejectsWhenTheHttpCallToGoogleFails() throws Exception {
        GoogleIdTokenVerifier delegate = mock(GoogleIdTokenVerifier.class);
        when(delegate.verify("any-token")).thenThrow(new IOException("network down"));

        GoogleTokenVerifier verifier = new GoogleTokenVerifier(delegate);

        assertThatThrownBy(() -> verifier.verify("any-token")).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void rejectsWhenSignatureVerificationThrows() throws Exception {
        GoogleIdTokenVerifier delegate = mock(GoogleIdTokenVerifier.class);
        when(delegate.verify("any-token")).thenThrow(new GeneralSecurityException("bad signature"));

        GoogleTokenVerifier verifier = new GoogleTokenVerifier(delegate);

        assertThatThrownBy(() -> verifier.verify("any-token")).isInstanceOf(BadCredentialsException.class);
    }
}
