package com.ibetcha.identity.application;

/**
 * Driving port: verifies an Apple identity token and extracts identity claims.
 * Implementations validate the JWT signature using Apple's public JWKS at
 * https://appleid.apple.com/auth/keys.
 *
 * Note: Apple only returns the email on the first sign-in. Subsequent logins
 * may have a null email — callers must handle this and look up the stored email.
 */
public interface AppleTokenVerifier {

    /**
     * Verify the identity token and return the extracted identity.
     * Throws {@link com.ibetcha.shared.exception.ApiException} (401) if invalid.
     */
    AppleIdentity verify(String identityToken);

    record AppleIdentity(String sub, String email) {}
}
