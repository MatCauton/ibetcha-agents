package com.ibetcha.identity.application;

import com.ibetcha.identity.domain.RefreshToken;
import com.ibetcha.identity.domain.User;
import com.ibetcha.identity.infrastructure.RefreshTokenRepository;
import com.ibetcha.identity.infrastructure.UserRepository;
import com.ibetcha.shared.exception.ApiException;
import com.ibetcha.shared.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final AppleTokenVerifier appleTokenVerifier;

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       JwtService jwtService,
                       PasswordEncoder passwordEncoder,
                       GoogleTokenVerifier googleTokenVerifier,
                       AppleTokenVerifier appleTokenVerifier) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.googleTokenVerifier = googleTokenVerifier;
        this.appleTokenVerifier = appleTokenVerifier;
    }

    @Transactional
    public AuthResult register(String email, String username, String password, String displayName) {
        if (userRepository.existsByEmail(email)) {
            throw ApiException.conflict("An account already exists for this email");
        }
        if (userRepository.existsByUsername(username)) {
            throw ApiException.conflict("Username already taken");
        }

        validatePassword(password);

        UUID userId = UUID.randomUUID();
        String hashedPassword = passwordEncoder.encode(password);
        User user = User.createEmailUser(userId, email, username, displayName, hashedPassword);
        userRepository.save(user);

        return issueTokens(user);
    }

    @Transactional
    public AuthResult loginWithGoogle(String idToken) {
        GoogleTokenVerifier.GoogleIdentity identity = googleTokenVerifier.verify(idToken);
        User user = findOrCreateOAuthUser(identity.email(), identity.name(),
                User.AuthProvider.GOOGLE, identity.sub());
        return issueTokens(user);
    }

    @Transactional
    public AuthResult loginWithApple(String identityToken) {
        AppleTokenVerifier.AppleIdentity identity = appleTokenVerifier.verify(identityToken);

        // Apple only sends email on first sign-in — look up by provider_id first
        Optional<User> existingBySub = userRepository.findByAuthProviderAndProviderId(
                User.AuthProvider.APPLE, identity.sub());

        if (existingBySub.isPresent()) {
            return issueTokens(existingBySub.get());
        }

        // First-time Apple sign-in must provide an email
        if (identity.email() == null) {
            throw ApiException.badRequest("Email is required on first Apple Sign-In");
        }

        User user = findOrCreateOAuthUser(identity.email(), null,
                User.AuthProvider.APPLE, identity.sub());
        return issueTokens(user);
    }

    private User findOrCreateOAuthUser(String email, String displayName,
                                        User.AuthProvider provider, String providerId) {
        // Prefer lookup by provider+providerId (most stable)
        Optional<User> byProvider = userRepository.findByAuthProviderAndProviderId(provider, providerId);
        if (byProvider.isPresent()) {
            return byProvider.get();
        }

        // Fall back to email lookup — existing account with same email from another provider
        Optional<User> byEmail = userRepository.findByEmail(email);
        if (byEmail.isPresent()) {
            throw ApiException.conflict("An account already exists for this email");
        }

        User user = User.createOAuthUser(UUID.randomUUID(), email, displayName, provider, providerId);
        return userRepository.save(user);
    }

    @Transactional
    public AuthResult login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.unauthorized("Incorrect email or password"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw ApiException.unauthorized("Incorrect email or password");
        }

        if (user.getAccountStatus() != User.AccountStatus.ACTIVE) {
            throw ApiException.unauthorized("Incorrect email or password");
        }

        return issueTokens(user);
    }

    @Transactional
    public AuthResult refresh(String refreshTokenString) {
        var claims = jwtService.parseToken(refreshTokenString);
        String jti = claims.getId();
        String type = claims.get("type", String.class);

        if (!"REFRESH".equals(type)) {
            throw ApiException.unauthorized("Invalid token type");
        }

        RefreshToken storedToken = refreshTokenRepository.findByJti(jti)
                .orElseThrow(() -> ApiException.unauthorized("Invalid refresh token"));

        if (storedToken.isRevoked()) {
            // Possible replay attack -- revoke entire family
            refreshTokenRepository.revokeFamily(storedToken.getTokenFamily());
            throw ApiException.unauthorized("Token has been revoked");
        }

        if (storedToken.isExpired()) {
            throw ApiException.unauthorized("Refresh token expired");
        }

        // Revoke old token
        storedToken.revoke();
        refreshTokenRepository.save(storedToken);

        // Issue new pair
        UUID userId = storedToken.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.unauthorized("User not found"));

        return issueTokens(user, storedToken.getTokenFamily());
    }

    @Transactional
    public void logout(String refreshTokenString) {
        try {
            var claims = jwtService.parseToken(refreshTokenString);
            String jti = claims.getId();
            refreshTokenRepository.findByJti(jti).ifPresent(token -> {
                token.revoke();
                refreshTokenRepository.save(token);
            });
        } catch (Exception e) {
            // Silently ignore invalid tokens on logout
        }
    }

    private AuthResult issueTokens(User user) {
        String family = UUID.randomUUID().toString();
        return issueTokens(user, family);
    }

    private AuthResult issueTokens(User user, String family) {
        String accessToken = jwtService.generateAccessToken(user.getId());
        JwtService.RefreshTokenData refreshData = jwtService.generateRefreshToken(user.getId(), family);

        RefreshToken storedRefresh = RefreshToken.create(
                user.getId(), refreshData.jti(), refreshData.family(), refreshData.expiresAt()
        );
        refreshTokenRepository.save(storedRefresh);

        return new AuthResult(
                accessToken,
                refreshData.token(),
                jwtService.getAccessTokenExpirySeconds(),
                new AuthResult.UserInfo(
                        user.getId(),
                        user.getEmail(),
                        user.getUsername(),
                        user.getDisplayName()
                )
        );
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw ApiException.badRequest("Password must be at least 8 characters and contain at least 1 number");
        }
        if (!password.matches(".*\\d.*")) {
            throw ApiException.badRequest("Password must be at least 8 characters and contain at least 1 number");
        }
    }

    public record AuthResult(
            String accessToken,
            String refreshToken,
            long expiresIn,
            UserInfo user
    ) {
        public record UserInfo(UUID id, String email, String username, String displayName) {}
    }
}
