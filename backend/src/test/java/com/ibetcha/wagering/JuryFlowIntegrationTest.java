package com.ibetcha.wagering;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibetcha.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the full jury flow.
 *
 * Test budget: 2 behaviors × 2 = 4 tests max.
 * Behaviors:
 *   B1 — Jury approves outcome → bet transitions to RESOLVED.
 *   B2 — Jury rejects outcome → bet returns to ACTIVE (claim cleared).
 *
 * Covers acceptance scenarios in [Feature: Jury System] — [JURY-01] and [JURY-02].
 */
class JuryFlowIntegrationTest extends IntegrationTestBase {

    @Autowired
    private ObjectMapper objectMapper;

    private String tomToken;
    private String mariaToken;
    private String alexToken;   // Alex is the jury
    private String tomUserId;
    private String mariaUserId;
    private String alexUserId;

    @BeforeEach
    void setupUsersAndFriendship() throws Exception {
        String suffix = String.valueOf(System.nanoTime());

        Map<String, Object> tomReg = registerUser("tom_jury_" + suffix + "@example.com",
                "tomj_" + suffix, "Str0ngPass1", "Tom Janssen");
        tomToken = (String) tomReg.get("accessToken");
        tomUserId = ((Map<String, Object>) tomReg.get("user")).get("id").toString();

        Map<String, Object> mariaReg = registerUser("maria_jury_" + suffix + "@example.com",
                "mariasantos_" + suffix, "Str0ngPass2", "Maria Santos");
        mariaToken = (String) mariaReg.get("accessToken");
        mariaUserId = ((Map<String, Object>) mariaReg.get("user")).get("id").toString();

        // Alex is the jury — he is a friend of Tom for the purposes of the bet,
        // but not a participant. He just needs to exist as a user.
        Map<String, Object> alexReg = registerUser("alex_jury_" + suffix + "@example.com",
                "alexpeeters_" + suffix, "Str0ngPass3", "Alex Peeters");
        alexToken = (String) alexReg.get("accessToken");
        alexUserId = ((Map<String, Object>) alexReg.get("user")).get("id").toString();

        // Tom sends friend request to Maria and Maria accepts
        MvcResult reqResult = mockMvc.perform(post("/api/v1/friends/request")
                        .header("Authorization", "Bearer " + tomToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetUserId": "%s"}
                                """.formatted(mariaUserId)))
                .andExpect(status().isCreated())
                .andReturn();
        String friendshipId = parseResponse(reqResult).get("requestId").toString();

        mockMvc.perform(post("/api/v1/friends/" + friendshipId + "/accept")
                        .header("Authorization", "Bearer " + mariaToken))
                .andExpect(status().isOk());
    }

    @Test
    void juryApproves_betTransitionsToResolved() throws Exception {
        String betId = createBetWithJuryAndActivate();

        // Tom claims victory
        mockMvc.perform(post("/api/v1/bets/" + betId + "/complete")
                        .header("Authorization", "Bearer " + tomToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"winnerId": "%s"}
                                """.formatted(tomUserId)))
                .andExpect(status().isOk());

        // Verify bet is now PENDING_JURY_VERDICT
        Map<String, Object> betState = parseResponse(mockMvc.perform(get("/api/v1/bets/" + betId)
                        .header("Authorization", "Bearer " + tomToken))
                .andExpect(status().isOk()).andReturn());
        assertThat(betState.get("status")).isEqualTo("PENDING_JURY_VERDICT");

        // Alex (jury) approves
        MvcResult verdictResult = mockMvc.perform(post("/api/v1/bets/" + betId + "/jury/verdict")
                        .header("Authorization", "Bearer " + alexToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"approved": true, "winnerId": "%s"}
                                """.formatted(tomUserId)))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> verdict = parseResponse(verdictResult);
        assertThat(verdict.get("status")).isEqualTo("RESOLVED");
        assertThat(verdict.get("approved")).isEqualTo(true);

        // Verify final state
        Map<String, Object> finalBet = parseResponse(mockMvc.perform(get("/api/v1/bets/" + betId)
                        .header("Authorization", "Bearer " + tomToken))
                .andExpect(status().isOk()).andReturn());
        assertThat(finalBet.get("status")).isEqualTo("RESOLVED");
        assertThat(finalBet.get("winnerId")).isEqualTo(tomUserId);
    }

    @Test
    void juryRejects_betReturnsToActive() throws Exception {
        String betId = createBetWithJuryAndActivate();

        // Tom claims victory
        mockMvc.perform(post("/api/v1/bets/" + betId + "/complete")
                        .header("Authorization", "Bearer " + tomToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"winnerId": "%s"}
                                """.formatted(tomUserId)))
                .andExpect(status().isOk());

        // Alex (jury) rejects
        MvcResult verdictResult = mockMvc.perform(post("/api/v1/bets/" + betId + "/jury/verdict")
                        .header("Authorization", "Bearer " + alexToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"approved": false}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> verdict = parseResponse(verdictResult);
        assertThat(verdict.get("status")).isEqualTo("ACTIVE");
        assertThat(verdict.get("approved")).isEqualTo(false);

        // Any participant can now submit a new claim
        Map<String, Object> betAfterRejection = parseResponse(mockMvc.perform(get("/api/v1/bets/" + betId)
                        .header("Authorization", "Bearer " + tomToken))
                .andExpect(status().isOk()).andReturn());
        assertThat(betAfterRejection.get("status")).isEqualTo("ACTIVE");
    }

    @Test
    void nonJuryUser_cannotSubmitVerdict_returnsForbidden() throws Exception {
        String betId = createBetWithJuryAndActivate();

        // Tom claims victory
        mockMvc.perform(post("/api/v1/bets/" + betId + "/complete")
                        .header("Authorization", "Bearer " + tomToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"winnerId": "%s"}
                                """.formatted(tomUserId)))
                .andExpect(status().isOk());

        // Maria (NOT the jury) tries to submit a verdict
        mockMvc.perform(post("/api/v1/bets/" + betId + "/jury/verdict")
                        .header("Authorization", "Bearer " + mariaToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"approved": true, "winnerId": "%s"}
                                """.formatted(tomUserId)))
                .andExpect(status().isForbidden());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String createBetWithJuryAndActivate() throws Exception {
        // Create bet with Alex as jury
        MvcResult betResult = mockMvc.perform(post("/api/v1/bets")
                        .header("Authorization", "Bearer " + tomToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "description": "5K race challenge",
                                    "stake": "Pizza",
                                    "participantIds": ["%s"],
                                    "juryUserId": "%s"
                                }
                                """.formatted(mariaUserId, alexUserId)))
                .andExpect(status().isCreated())
                .andReturn();
        String betId = parseResponse(betResult).get("betId").toString();

        // Maria accepts
        mockMvc.perform(post("/api/v1/bets/" + betId + "/accept")
                        .header("Authorization", "Bearer " + mariaToken))
                .andExpect(status().isOk());

        return betId;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> registerUser(String email, String username,
                                              String password, String displayName) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "%s",
                                    "username": "%s",
                                    "password": "%s",
                                    "displayName": "%s"
                                }
                                """.formatted(email, username, password, displayName)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseResponse(MvcResult result) throws Exception {
        return objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
    }
}
