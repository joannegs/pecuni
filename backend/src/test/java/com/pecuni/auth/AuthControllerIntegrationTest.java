package com.pecuni.auth;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import tools.jackson.databind.JsonNode;
import com.pecuni.AbstractIntegrationTest;
import com.pecuni.auth.dto.GoogleLoginRequest;
import com.pecuni.auth.dto.LoginRequest;
import com.pecuni.auth.dto.RefreshRequest;
import com.pecuni.auth.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

class AuthControllerIntegrationTest extends AbstractIntegrationTest {

    @MockitoBean
    private GoogleTokenVerifier googleTokenVerifier;

    @Autowired
    private com.pecuni.user.UserRepository userRepository;

    private String uniqueEmail() {
        return "user-" + java.util.UUID.randomUUID() + "@email.com";
    }

    private JsonNode registerUser(String email, String senha) throws Exception {
        RegisterRequest request = new RegisterRequest("Ana Souza", email, senha);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    @Test
    void registrarCreatesUserWorkspaceAndReturnsTokenPair() throws Exception {
        registerUser(uniqueEmail(), "S3nhaForte!");
    }

    @Test
    void registrarWithDuplicateEmailReturns409() throws Exception {
        String email = uniqueEmail();
        registerUser(email, "S3nhaForte!");

        RegisterRequest duplicate = new RegisterRequest("Outro Nome", email, "OutraSenha1!");

        mockMvc.perform(post("/api/v1/auth/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicate)))
                .andExpect(status().isConflict())
                .andExpect(header().string("Content-Type", containsString("application/problem+json")))
                .andExpect(jsonPath("$.type").value(containsString("email-already-registered")));
    }

    @Test
    void registrarWithInvalidPayloadReturns400() throws Exception {
        RegisterRequest invalid = new RegisterRequest("", "not-an-email", "short");

        mockMvc.perform(post("/api/v1/auth/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(containsString("validacao")))
                .andExpect(jsonPath("$.erros").isArray());
    }

    @Test
    void loginWithCorrectCredentialsSucceeds() throws Exception {
        String email = uniqueEmail();
        registerUser(email, "S3nhaForte!");

        LoginRequest login = new LoginRequest(email, "S3nhaForte!");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void loginWithWrongPasswordReturns401WithGenericMessage() throws Exception {
        String email = uniqueEmail();
        registerUser(email, "S3nhaForte!");

        LoginRequest login = new LoginRequest(email, "SenhaErrada!");
        String wrongPasswordDetail = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        LoginRequest nonExistent = new LoginRequest(uniqueEmail(), "QualquerSenha1!");
        String nonExistentEmailDetail = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nonExistent)))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        // Same message for both cases — never leak which emails are registered.
        org.assertj.core.api.Assertions.assertThat(objectMapper.readTree(wrongPasswordDetail).get("detail"))
                .isEqualTo(objectMapper.readTree(nonExistentEmailDetail).get("detail"));
    }

    @Test
    void loginWithGoogleCreatesANewUserWhenEmailIsUnknown() throws Exception {
        String email = uniqueEmail();
        Mockito.when(googleTokenVerifier.verify("valid-google-token"))
                .thenReturn(new GoogleTokenVerifier.GoogleUserInfo(email, "Google User"));

        GoogleLoginRequest request = new GoogleLoginRequest("valid-google-token");
        mockMvc.perform(post("/api/v1/auth/login/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());

        org.assertj.core.api.Assertions.assertThat(userRepository.findByEmail(email)).isPresent();
        org.assertj.core.api.Assertions.assertThat(userRepository.findByEmail(email).get().getProvider())
                .isEqualTo(com.pecuni.user.AuthProvider.GOOGLE);
    }

    @Test
    void loginWithGoogleLinksToAnExistingLocalAccountInsteadOfDuplicating() throws Exception {
        String email = uniqueEmail();
        registerUser(email, "S3nhaForte!");

        Mockito.when(googleTokenVerifier.verify("valid-google-token"))
                .thenReturn(new GoogleTokenVerifier.GoogleUserInfo(email, "Ana Souza"));

        GoogleLoginRequest request = new GoogleLoginRequest("valid-google-token");
        mockMvc.perform(post("/api/v1/auth/login/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        long count = userRepository.findAll().stream().filter(u -> u.getEmail().equals(email)).count();
        org.assertj.core.api.Assertions.assertThat(count).isEqualTo(1);
    }

    @Test
    void refreshRotatesTheTokenAndRejectsReuseOfTheOldOne() throws Exception {
        JsonNode tokens = registerUser(uniqueEmail(), "S3nhaForte!");
        String originalRefreshToken = tokens.get("refreshToken").asText();

        RefreshRequest refreshRequest = new RefreshRequest(originalRefreshToken);
        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshToken").value(org.hamcrest.Matchers.not(originalRefreshToken)))
                .andReturn();

        // Reusing the now-rotated-out token must fail.
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isUnauthorized());

        // The freshly issued one still works.
        JsonNode newTokens = objectMapper.readTree(refreshResult.getResponse().getContentAsString());
        RefreshRequest secondRefresh = new RefreshRequest(newTokens.get("refreshToken").asText());
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondRefresh)))
                .andExpect(status().isOk());
    }

    @Test
    void refreshWithAnUnknownTokenReturns401() throws Exception {
        RefreshRequest request = new RefreshRequest("nonexistent-refresh-token");
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevokesTheRefreshTokenSoItCanNoLongerBeUsed() throws Exception {
        JsonNode tokens = registerUser(uniqueEmail(), "S3nhaForte!");
        String refreshToken = tokens.get("refreshToken").asText();

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest(refreshToken))))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest(refreshToken))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meReturnsTheAuthenticatedUserFromTheAccessToken() throws Exception {
        String email = uniqueEmail();
        JsonNode tokens = registerUser(email, "S3nhaForte!");
        String accessToken = tokens.get("accessToken").asText();

        mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Ana Souza"))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.provider").value("LOCAL"));
    }

    @Test
    void meWithoutATokenReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Content-Type", containsString("application/problem+json")));
    }

    @Test
    void meWithAnInvalidTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer garbage-token"))
                .andExpect(status().isUnauthorized());
    }
}
