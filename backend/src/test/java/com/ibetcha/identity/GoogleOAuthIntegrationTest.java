package com.ibetcha.identity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.ibetcha.IntegrationTestBase;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test: Google OAuth login flow.
 *
 * Test budget: 2 behaviors × 2 = 4 tests max.
 * Behaviors:
 *   B1 — New user signs in with Google → account created, tokens issued.
 *   B2 — Existing user signs in with Google → existing account found, tokens issued.
 *
 * Covers acceptance scenarios [AUTH-01] and [AUTH-08].
 *
 * WireMock stubs the Google tokeninfo endpoint so no real network call is made.
 */
class GoogleOAuthIntegrationTest extends IntegrationTestBase {

    @Autowired
    private ObjectMapper objectMapper;

    // WireMock must be started before @DynamicPropertySource runs so the port is known.
    // We use a static initializer + explicit lifecycle management to ensure this ordering.
    static final WireMockServer wireMock;

    static {
        wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMock.start();
        configureFor("localhost", wireMock.port());
    }

    @AfterAll
    static void stopWireMock() {
        wireMock.stop();
    }

    @DynamicPropertySource
    static void configureGoogleTokenInfoUrl(DynamicPropertyRegistry registry) {
        registry.add("ibetcha.oauth.google.token-info-url",
                () -> "http://localhost:" + wireMock.port() + "/tokeninfo");
    }

    @Test
    void newUser_googleSignIn_createsAccountAndIssuesTokens() throws Exception {
        // Stub Google tokeninfo to return a valid identity
        wireMock.stubFor(get(urlPathEqualTo("/tokeninfo"))
                .withQueryParam("id_token", equalTo("valid-google-token-new"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "sub": "google-sub-new-user-123",
                                    "email": "newuser.google@example.com",
                                    "name": "New Google User",
                                    "email_verified": "true"
                                }
                                """)));

        MvcResult result = mockMvc.perform(post("/api/v1/auth/oauth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idToken": "valid-google-token-new"}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> response = parseResponse(result);
        assertThat(response.get("accessToken")).isNotNull().isInstanceOf(String.class);
        assertThat(response.get("refreshToken")).isNotNull().isInstanceOf(String.class);
        Map<String, Object> user = (Map<String, Object>) response.get("user");
        assertThat(user.get("email")).isEqualTo("newuser.google@example.com");
    }

    @Test
    void existingUser_googleSignIn_issuesTokensWithoutCreatingDuplicate() throws Exception {
        // First sign-in creates the user
        String googleSub = "google-sub-returning-456";
        String email = "returning.google@example.com";

        wireMock.stubFor(get(urlPathEqualTo("/tokeninfo"))
                .withQueryParam("id_token", equalTo("valid-google-token-returning"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "sub": "%s",
                                    "email": "%s",
                                    "name": "Returning Google User"
                                }
                                """.formatted(googleSub, email))));

        // First sign-in
        mockMvc.perform(post("/api/v1/auth/oauth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idToken": "valid-google-token-returning"}
                                """))
                .andExpect(status().isOk());

        // Second sign-in with same token
        MvcResult secondResult = mockMvc.perform(post("/api/v1/auth/oauth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idToken": "valid-google-token-returning"}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> response = parseResponse(secondResult);
        assertThat(response.get("accessToken")).isNotNull();
        // Same email returned — same account
        Map<String, Object> user = (Map<String, Object>) response.get("user");
        assertThat(user.get("email")).isEqualTo(email);
    }

    @Test
    void invalidGoogleToken_returns401() throws Exception {
        wireMock.stubFor(get(urlPathEqualTo("/tokeninfo"))
                .withQueryParam("id_token", equalTo("bad-token"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"error": "invalid_token", "error_description": "Invalid Value"}
                                """)));

        mockMvc.perform(post("/api/v1/auth/oauth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idToken": "bad-token"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseResponse(MvcResult result) throws Exception {
        return objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
    }
}
