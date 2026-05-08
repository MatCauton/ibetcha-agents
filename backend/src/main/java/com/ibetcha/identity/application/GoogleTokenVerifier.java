package com.ibetcha.identity.application;

/**
 * Driving port: verifies a Google ID token and extracts identity claims.
 * Implementations call https://oauth2.googleapis.com/tokeninfo or use
 * Google's JWKS endpoint for offline verification.
 */
public interface GoogleTokenVerifier {

    /**
     * Verify the token and return the extracted identity.
     * Throws {@link com.ibetcha.shared.exception.ApiException} (401) if the token is invalid.
     */
    GoogleIdentity verify(String idToken);

    record GoogleIdentity(String sub, String email, String name) {}
}
