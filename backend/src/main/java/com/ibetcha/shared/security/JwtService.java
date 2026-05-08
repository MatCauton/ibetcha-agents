package com.ibetcha.shared.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    @Value("${ibetcha.jwt.issuer}")
    private String issuer;

    @Value("${ibetcha.jwt.access-token-expiry-minutes}")
    private long accessTokenExpiryMinutes;

    @Value("${ibetcha.jwt.refresh-token-expiry-days}")
    private long refreshTokenExpiryDays;

    private PrivateKey privateKey;
    private PublicKey publicKey;

    @PostConstruct
    public void init() {
        // MVP: generates a new key pair on every startup — all existing tokens are invalidated on redeploy.
        // Before production launch: load a stable key pair from AWS Secrets Manager and inject via @Value.
        KeyPair keyPair = Jwts.SIG.ES256.keyPair().build();
        this.privateKey = keyPair.getPrivate();
        this.publicKey = keyPair.getPublic();
    }

    public String generateAccessToken(UUID userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(issuer)
                .subject(userId.toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(Duration.ofMinutes(accessTokenExpiryMinutes))))
                .id(UUID.randomUUID().toString())
                .claim("type", "ACCESS")
                .signWith(privateKey)
                .compact();
    }

    public RefreshTokenData generateRefreshToken(UUID userId, String tokenFamily) {
        Instant now = Instant.now();
        String jti = UUID.randomUUID().toString();
        Instant expiresAt = now.plus(Duration.ofDays(refreshTokenExpiryDays));

        String token = Jwts.builder()
                .issuer(issuer)
                .subject(userId.toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .id(jti)
                .claim("type", "REFRESH")
                .claim("family", tokenFamily)
                .signWith(privateKey)
                .compact();

        return new RefreshTokenData(token, jti, tokenFamily, expiresAt);
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(publicKey)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UUID extractUserId(String token) {
        Claims claims = parseToken(token);
        return UUID.fromString(claims.getSubject());
    }

    public boolean isAccessToken(Claims claims) {
        return "ACCESS".equals(claims.get("type", String.class));
    }

    public long getAccessTokenExpirySeconds() {
        return accessTokenExpiryMinutes * 60;
    }

    public record RefreshTokenData(String token, String jti, String family, Instant expiresAt) {}
}
