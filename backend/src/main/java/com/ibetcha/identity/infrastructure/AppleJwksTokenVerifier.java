package com.ibetcha.identity.infrastructure;

import com.ibetcha.identity.application.AppleTokenVerifier;
import com.ibetcha.shared.exception.ApiException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URL;

/**
 * Adapter: verifies Apple identity tokens by validating their JWT signature
 * against Apple's public JWKS at https://appleid.apple.com/auth/keys.
 */
@Component
public class AppleJwksTokenVerifier implements AppleTokenVerifier {

    private static final Logger log = LoggerFactory.getLogger(AppleJwksTokenVerifier.class);

    private final String jwksUrl;
    private final String expectedIssuer;

    public AppleJwksTokenVerifier(
            @Value("${ibetcha.oauth.apple.jwks-url:https://appleid.apple.com/auth/keys}") String jwksUrl,
            @Value("${ibetcha.oauth.apple.issuer:https://appleid.apple.com}") String expectedIssuer) {
        this.jwksUrl = jwksUrl;
        this.expectedIssuer = expectedIssuer;
    }

    @Override
    public AppleIdentity verify(String identityToken) {
        try {
            JWKSource<SecurityContext> keySource = new RemoteJWKSet<>(new URL(jwksUrl));
            JWSKeySelector<SecurityContext> keySelector =
                    new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, keySource);

            ConfigurableJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
            processor.setJWSKeySelector(keySelector);

            JWTClaimsSet claims = processor.process(identityToken, null);

            if (!expectedIssuer.equals(claims.getIssuer())) {
                throw ApiException.unauthorized("Apple token issuer mismatch");
            }

            String sub = claims.getSubject();
            String email = claims.getStringClaim("email");

            if (sub == null) {
                throw ApiException.unauthorized("Apple token missing sub claim");
            }

            return new AppleIdentity(sub, email);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Apple token verification failed: {}", e.getMessage());
            throw ApiException.unauthorized("Apple token verification failed");
        }
    }
}
