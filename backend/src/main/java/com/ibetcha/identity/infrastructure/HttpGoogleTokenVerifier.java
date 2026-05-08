package com.ibetcha.identity.infrastructure;

import com.ibetcha.identity.application.GoogleTokenVerifier;
import com.ibetcha.shared.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Adapter: verifies Google ID tokens by calling Google's tokeninfo endpoint.
 * For production, prefer offline verification using Google's JWKS, but the
 * tokeninfo endpoint is simpler and sufficient for MVP.
 */
@Component
public class HttpGoogleTokenVerifier implements GoogleTokenVerifier {

    private static final Logger log = LoggerFactory.getLogger(HttpGoogleTokenVerifier.class);

    private final RestTemplate restTemplate;
    private final String tokenInfoUrl;

    public HttpGoogleTokenVerifier(
            @Value("${ibetcha.oauth.google.token-info-url:https://oauth2.googleapis.com/tokeninfo}") String tokenInfoUrl) {
        this.restTemplate = new RestTemplate();
        this.tokenInfoUrl = tokenInfoUrl;
    }

    @Override
    @SuppressWarnings("unchecked")
    public GoogleIdentity verify(String idToken) {
        String url = tokenInfoUrl + "?id_token=" + idToken;
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            Map<String, Object> body = response.getBody();
            if (body == null || body.containsKey("error")) {
                throw ApiException.unauthorized("Invalid Google ID token");
            }
            String sub = (String) body.get("sub");
            String email = (String) body.get("email");
            String name = (String) body.getOrDefault("name", "");
            if (sub == null || email == null) {
                throw ApiException.unauthorized("Google token missing required claims");
            }
            return new GoogleIdentity(sub, email, name);
        } catch (ApiException e) {
            throw e;
        } catch (HttpClientErrorException e) {
            // Google returns 400 for invalid tokens
            log.warn("Google token rejected (HTTP {}): {}", e.getStatusCode(), e.getMessage());
            throw ApiException.unauthorized("Invalid Google ID token");
        } catch (Exception e) {
            log.warn("Google token verification failed: {}", e.getMessage());
            throw ApiException.unauthorized("Google token verification failed");
        }
    }
}
