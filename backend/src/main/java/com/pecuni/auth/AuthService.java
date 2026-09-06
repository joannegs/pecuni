package com.pecuni.auth;

import com.pecuni.auth.dto.AuthResponse;
import com.pecuni.auth.dto.GoogleLoginRequest;
import com.pecuni.auth.dto.LoginRequest;
import com.pecuni.auth.dto.RefreshRequest;
import com.pecuni.auth.dto.RegisterRequest;
import com.pecuni.common.exception.BusinessRuleException;
import com.pecuni.common.exception.ResourceNotFoundException;
import com.pecuni.user.AuthProvider;
import com.pecuni.user.User;
import com.pecuni.user.UserRepository;
import com.pecuni.workspace.Workspace;
import com.pecuni.workspace.WorkspaceMember;
import com.pecuni.workspace.WorkspaceMemberRepository;
import com.pecuni.workspace.WorkspaceRepository;
import com.pecuni.workspace.WorkspaceRole;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final GoogleTokenVerifier googleTokenVerifier;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessRuleException("email-already-registered", "This email is already registered.");
        }

        User user = new User();
        user.setName(request.nome());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.senha()));
        user.setProvider(AuthProvider.LOCAL);
        userRepository.save(user);

        createPersonalWorkspace(user);

        return issueTokenPair(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email()).orElseThrow(this::invalidCredentials);
        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.senha(), user.getPasswordHash())) {
            throw invalidCredentials();
        }
        return issueTokenPair(user);
    }

    public AuthResponse loginWithGoogle(GoogleLoginRequest request) {
        GoogleTokenVerifier.GoogleUserInfo googleUser = googleTokenVerifier.verify(request.idToken());

        User user = userRepository.findByEmail(googleUser.email()).orElseGet(() -> registerGoogleUser(googleUser));

        return issueTokenPair(user);
    }

    public AuthResponse refresh(RefreshRequest request) {
        RefreshTokenService.RotationResult result = refreshTokenService.rotate(request.refreshToken());
        User user = result.user();
        String accessToken = jwtService.generateAccessToken(user.getId(), workspaceIdOf(user), user.getEmail());
        return new AuthResponse(accessToken, result.rawRefreshToken(), jwtService.getAccessTokenTtlSeconds());
    }

    public void logout(RefreshRequest request) {
        refreshTokenService.revoke(request.refreshToken());
    }

    @Transactional(readOnly = true)
    public User getCurrentUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
    }

    private User registerGoogleUser(GoogleTokenVerifier.GoogleUserInfo googleUser) {
        User user = new User();
        user.setName(googleUser.name());
        user.setEmail(googleUser.email());
        user.setProvider(AuthProvider.GOOGLE);
        userRepository.save(user);
        createPersonalWorkspace(user);
        return user;
    }

    private void createPersonalWorkspace(User user) {
        Workspace workspace = new Workspace();
        workspace.setName("Workspace de " + user.getName());
        workspaceRepository.save(workspace);

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        member.setUser(user);
        member.setRole(WorkspaceRole.OWNER);
        workspaceMemberRepository.save(member);
    }

    private AuthResponse issueTokenPair(User user) {
        String accessToken = jwtService.generateAccessToken(user.getId(), workspaceIdOf(user), user.getEmail());
        String rawRefreshToken = refreshTokenService.issue(user);
        return new AuthResponse(accessToken, rawRefreshToken, jwtService.getAccessTokenTtlSeconds());
    }

    private UUID workspaceIdOf(User user) {
        return workspaceMemberRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("Usuário sem workspace associado: " + user.getId()))
                .getWorkspace()
                .getId();
    }

    private BadCredentialsException invalidCredentials() {
        // Same message for "email doesn't exist" and "wrong password" — never confirm which emails are registered.
        return new BadCredentialsException("E-mail ou senha inválidos.");
    }
}
