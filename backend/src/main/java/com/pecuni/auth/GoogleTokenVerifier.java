package com.pecuni.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;

/**
 * Isolates the call to Google's servers. Validates the id token's signature,
 * audience (our OAuth2 client ID) and expiry directly with Google — the
 * caller must never trust an email/name sent loose by the client (contract,
 * section 2).
 *
 * <p>Verification approach: {@code google-api-client}'s {@link GoogleIdTokenVerifier}
 * rather than a hand-rolled JWKS fetch/cache/verify — it's the library Google
 * ships for exactly this, handles JWKS caching and rotation, and needs far
 * less code than a manual implementation. See {@code backend/README.md}.
 */
@Component
public class GoogleTokenVerifier {

    private final GoogleIdTokenVerifier verifier;

    @Autowired
    public GoogleTokenVerifier(GoogleProperties googleProperties) {
        this(new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(googleProperties.clientId()))
                .build());
    }

    /** Package-private seam for unit tests — see {@code GoogleTokenVerifierTest}. */
    GoogleTokenVerifier(GoogleIdTokenVerifier verifier) {
        this.verifier = verifier;
    }

    public GoogleUserInfo verify(String idToken) {
        GoogleIdToken googleIdToken;
        try {
            googleIdToken = verifier.verify(idToken);
        } catch (GeneralSecurityException | IOException ex) {
            throw new BadCredentialsException("Não foi possível validar o token do Google.");
        }
        if (googleIdToken == null) {
            throw new BadCredentialsException("Token do Google inválido.");
        }
        GoogleIdToken.Payload payload = googleIdToken.getPayload();
        String email = payload.getEmail();
        Object name = payload.get("name");
        return new GoogleUserInfo(email, name != null ? name.toString() : email);
    }

    public record GoogleUserInfo(String email, String name) {
    }
}
