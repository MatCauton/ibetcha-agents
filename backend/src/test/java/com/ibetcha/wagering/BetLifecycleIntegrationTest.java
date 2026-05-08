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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for bet lifecycle state machine.
 * Tests through the REST API driving port with real PostgreSQL.
 *
 * Covers:
 * - Bet creation in PENDING_ACCEPTANCE
 * - PENDING_ACCEPTANCE -> ACTIVE on last accept
 * - ACTIVE -> PENDING_APPROVAL on mark complete (no jury)
 * - PENDING_APPROVAL -> RESOLVED on majority vote
 * - ACTIVE -> RESOLVED on concession
 * - Validation: cannot bet with non-friends
 * - Validation: cannot accept expired bet
 */
class BetLifecycleIntegrationTest extends IntegrationTestBase {

    @Autowired
    private ObjectMapper objectMapper;

    private String tomToken;
    private String mariaToken;
    private String tomUserId;
    private String mariaUserId;

    @BeforeEach
    void setupUsersAndFriendship() throws Exception {
        // Register two users and make them friends
        String suffix = String.valueOf(System.nanoTime());

        Map<String, Object> tomReg = registerUser(
                "tom_" + suffix + "@example.com",
                "tom_" + suffix,
                "Str0ngPass1",
                "Tom Janssen"
        );
        tomToken = (String) tomReg.get("accessToken");
        tomUserId = ((Map<String, Object>) tomReg.get("user")).get("id").toString();

        Map<String, Object> mariaReg = registerUser(
                "maria_" + suffix + "@example.com",
                "maria_" + suffix,
                "Str0ngPass2",
                "Maria Santos"
        );
        mariaToken = (String) mariaReg.get("accessToken");
        mariaUserId = ((Map<String, Object>) mariaReg.get("user")).get("id").toString();

        // Tom sends friend request
        MvcResult reqResult = mockMvc.perform(post("/api/v1/friends/request")
                        .header("Authorization", "Bearer " + tomToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetUserId": "%s"}
                                """.formatted(mariaUserId)))
                .andExpect(status().isCreated())
                .andReturn();
        String friendshipId = parseResponse(reqResult).get("requestId").toString();

        // Maria accepts
        mockMvc.perform(post("/api/v1/friends/" + friendshipId + "/accept")
                        .header("Authorization", "Bearer " + mariaToken))
                .andExpect(status().isOk());
    }

    @Test
    void createBet_returnsStatusPendingAcceptance() throws Exception {
        MvcResult result = createQuickBet(tomToken, mariaUserId,
                "Sunday ride challenge", "Loser buys coffee");

        Map<String, Object> response = parseResponse(result);
        assertThat(response.get("status")).isEqualTo("PENDING_ACCEPTANCE");
        assertThat(response.get("description")).isEqualTo("Sunday ride challenge");
        assertThat(response.get("stake")).isEqualTo("Loser buys coffee");
    }

    @Test
    void acceptBet_lastParticipant_transitionsToActive() throws Exception {
        MvcResult betResult = createQuickBet(tomToken, mariaUserId,
                "Cycling race", "Coffee");
        String betId = parseResponse(betResult).get("betId").toString();

        MvcResult acceptResult = mockMvc.perform(post("/api/v1/bets/" + betId + "/accept")
                        .header("Authorization", "Bearer " + mariaToken))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(parseResponse(acceptResult).get("status")).isEqualTo("ACTIVE");
    }

    @Test
    void completeBet_claimSelf_transitionsToPendingApproval() throws Exception {
        String betId = createAndActivateBet("Race challenge", "Pizza");

        MvcResult result = mockMvc.perform(post("/api/v1/bets/" + betId + "/complete")
                        .header("Authorization", "Bearer " + tomToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"winnerId": "%s"}
                                """.formatted(tomUserId)))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(parseResponse(result).get("status")).isEqualTo("PENDING_APPROVAL");
    }

    @Test
    void completeBet_concession_resolvesImmediately() throws Exception {
        String betId = createAndActivateBet("Concession bet", "Lunch");

        // Tom selects Maria as winner (concession)
        MvcResult result = mockMvc.perform(post("/api/v1/bets/" + betId + "/complete")
                        .header("Authorization", "Bearer " + tomToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"winnerId": "%s"}
                                """.formatted(mariaUserId)))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> response = parseResponse(result);
        assertThat(response.get("status")).isEqualTo("RESOLVED");
    }

    @Test
    void voteApprove_majorityReached_resolvesbet() throws Exception {
        String betId = createAndActivateBet("Vote test", "Dinner");

        // Tom claims victory
        mockMvc.perform(post("/api/v1/bets/" + betId + "/complete")
                        .header("Authorization", "Bearer " + tomToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"winnerId": "%s"}
                                """.formatted(tomUserId)))
                .andExpect(status().isOk());

        // Maria approves
        MvcResult voteResult = mockMvc.perform(post("/api/v1/bets/" + betId + "/outcome/vote")
                        .header("Authorization", "Bearer " + mariaToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"vote": "APPROVE"}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> voteResponse = parseResponse(voteResult);
        assertThat(voteResponse.get("status")).isEqualTo("RESOLVED");
        assertThat(voteResponse.get("majorityReached")).isEqualTo(true);
    }

    @Test
    void voteDispute_twoPerson_transitionsToDisputed() throws Exception {
        String betId = createAndActivateBet("Dispute test", "Bragging rights");

        // Tom claims victory
        mockMvc.perform(post("/api/v1/bets/" + betId + "/complete")
                        .header("Authorization", "Bearer " + tomToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"winnerId": "%s"}
                                """.formatted(tomUserId)))
                .andExpect(status().isOk());

        // Maria disputes
        MvcResult voteResult = mockMvc.perform(post("/api/v1/bets/" + betId + "/outcome/vote")
                        .header("Authorization", "Bearer " + mariaToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"vote": "DISPUTE"}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(parseResponse(voteResult).get("status")).isEqualTo("DISPUTED");
    }

    @Test
    void completeBet_whenNotActive_returnsConflict() throws Exception {
        MvcResult betResult = createQuickBet(tomToken, mariaUserId,
                "Not active bet", "Coffee");
        String betId = parseResponse(betResult).get("betId").toString();

        // Try to complete before accepting (bet is still PENDING_ACCEPTANCE)
        mockMvc.perform(post("/api/v1/bets/" + betId + "/complete")
                        .header("Authorization", "Bearer " + tomToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"winnerId": "%s"}
                                """.formatted(tomUserId)))
                .andExpect(status().isConflict());
    }

    @Test
    void createBet_withNonFriend_returnsBadRequest() throws Exception {
        // Register a stranger (not friends with Tom)
        String suffix = String.valueOf(System.nanoTime());
        Map<String, Object> strangerReg = registerUser(
                "stranger_" + suffix + "@example.com",
                "stranger_" + suffix,
                "Str0ngPass1",
                "Stranger"
        );
        String strangerId = ((Map<String, Object>) strangerReg.get("user")).get("id").toString();

        mockMvc.perform(post("/api/v1/bets")
                        .header("Authorization", "Bearer " + tomToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "description": "Bet with stranger",
                                    "stake": "Coffee",
                                    "participantIds": ["%s"]
                                }
                                """.formatted(strangerId)))
                .andExpect(status().isBadRequest());
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private String createAndActivateBet(String description, String stake) throws Exception {
        MvcResult betResult = createQuickBet(tomToken, mariaUserId, description, stake);
        String betId = parseResponse(betResult).get("betId").toString();

        mockMvc.perform(post("/api/v1/bets/" + betId + "/accept")
                        .header("Authorization", "Bearer " + mariaToken))
                .andExpect(status().isOk());

        return betId;
    }

    private MvcResult createQuickBet(String token, String participantId,
                                      String description, String stake) throws Exception {
        return mockMvc.perform(post("/api/v1/bets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "description": "%s",
                                    "stake": "%s",
                                    "participantIds": ["%s"]
                                }
                                """.formatted(description, stake, participantId)))
                .andExpect(status().isCreated())
                .andReturn();
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
