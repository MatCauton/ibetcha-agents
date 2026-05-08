package com.ibetcha;

import com.fasterxml.jackson.databind.ObjectMapper;
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
 * Walking Skeleton: Register -> Add Friend -> Quick Bet -> Accept -> Resolve.
 *
 * This test exercises the full end-to-end flow through the REST API using
 * a real PostgreSQL database (Testcontainers). No mocks.
 *
 * Covers acceptance scenarios:
 *   [WS-AUTH-01]  (simplified to email/password for backend)
 *   [WS-SOCIAL-01]
 *   [WS-BET-01]
 *   [WS-ACCEPT-01]
 *   [WS-COMPLETE-01]
 */
class WalkingSkeletonTest extends IntegrationTestBase {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void fullWalkingSkeleton_register_addFriend_createBet_accept_resolve() throws Exception {
        // ── Step 1: Register Tom ────────────────────────────────────────────
        String tomToken = registerAndGetToken("tom@example.com", "tomj", "Str0ngPass1", "Tom Janssen");

        // ── Step 2: Register Maria ─────────────────────────────────────────
        String mariaToken = registerAndGetToken("maria@example.com", "mariasantos", "Str0ngPass2", "Maria Santos");

        // ── Step 3: Tom searches for Maria ─────────────────────────────────
        MvcResult searchResult = mockMvc.perform(get("/api/v1/friends/search")
                        .param("q", "maria")
                        .header("Authorization", "Bearer " + tomToken))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> searchResponse = parseResponse(searchResult);
        List<Map<String, Object>> results = (List<Map<String, Object>>) searchResponse.get("results");
        assertThat(results).isNotEmpty();

        String mariaUserId = results.stream()
                .filter(r -> "mariasantos".equals(r.get("username")))
                .findFirst()
                .map(r -> r.get("userId").toString())
                .orElseThrow();

        // ── Step 4: Tom sends friend request to Maria ──────────────────────
        MvcResult requestResult = mockMvc.perform(post("/api/v1/friends/request")
                        .header("Authorization", "Bearer " + tomToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetUserId": "%s"}
                                """.formatted(mariaUserId)))
                .andExpect(status().isCreated())
                .andReturn();

        Map<String, Object> requestResponse = parseResponse(requestResult);
        String friendshipId = requestResponse.get("requestId").toString();

        // ── Step 5: Maria accepts friend request ───────────────────────────
        mockMvc.perform(post("/api/v1/friends/" + friendshipId + "/accept")
                        .header("Authorization", "Bearer " + mariaToken))
                .andExpect(status().isOk());

        // Verify both see each other in friends list
        MvcResult tomFriends = mockMvc.perform(get("/api/v1/friends")
                        .header("Authorization", "Bearer " + tomToken))
                .andExpect(status().isOk())
                .andReturn();
        Map<String, Object> tomFriendsResponse = parseResponse(tomFriends);
        List<Map<String, Object>> tomFriendsList = (List<Map<String, Object>>) tomFriendsResponse.get("friends");
        assertThat(tomFriendsList).hasSize(1);
        assertThat(tomFriendsList.get(0).get("username")).isEqualTo("mariasantos");

        // ── Step 6: Tom creates a quick bet with Maria ─────────────────────
        MvcResult betResult = mockMvc.perform(post("/api/v1/bets")
                        .header("Authorization", "Bearer " + tomToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "description": "I'll beat you on Sunday's ride",
                                    "stake": "Loser buys coffee",
                                    "participantIds": ["%s"]
                                }
                                """.formatted(mariaUserId)))
                .andExpect(status().isCreated())
                .andReturn();

        Map<String, Object> betResponse = parseResponse(betResult);
        assertThat(betResponse.get("status")).isEqualTo("PENDING_ACCEPTANCE");
        assertThat(betResponse.get("description")).isEqualTo("I'll beat you on Sunday's ride");
        assertThat(betResponse.get("stake")).isEqualTo("Loser buys coffee");
        String betId = betResponse.get("betId").toString();

        // ── Step 7: Maria accepts the bet ──────────────────────────────────
        MvcResult acceptResult = mockMvc.perform(post("/api/v1/bets/" + betId + "/accept")
                        .header("Authorization", "Bearer " + mariaToken))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> acceptResponse = parseResponse(acceptResult);
        assertThat(acceptResponse.get("status")).isEqualTo("ACTIVE");

        // ── Step 8: Tom declares himself winner ────────────────────────────
        // Extract Tom's userId
        MvcResult tomBetDetail = mockMvc.perform(get("/api/v1/bets/" + betId)
                        .header("Authorization", "Bearer " + tomToken))
                .andExpect(status().isOk())
                .andReturn();
        Map<String, Object> betDetail = parseResponse(tomBetDetail);
        List<Map<String, Object>> participants = (List<Map<String, Object>>) betDetail.get("participants");
        String tomUserId = participants.stream()
                .filter(p -> "CREATOR".equals(p.get("role")))
                .findFirst()
                .map(p -> p.get("userId").toString())
                .orElseThrow();

        MvcResult completeResult = mockMvc.perform(post("/api/v1/bets/" + betId + "/complete")
                        .header("Authorization", "Bearer " + tomToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"winnerId": "%s"}
                                """.formatted(tomUserId)))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> completeResponse = parseResponse(completeResult);
        assertThat(completeResponse.get("status")).isEqualTo("PENDING_APPROVAL");

        // ── Step 9: Maria approves the outcome ─────────────────────────────
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

        // ── Step 10: Verify bet is resolved with Tom as winner ─────────────
        MvcResult finalBet = mockMvc.perform(get("/api/v1/bets/" + betId)
                        .header("Authorization", "Bearer " + tomToken))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> finalBetResponse = parseResponse(finalBet);
        assertThat(finalBetResponse.get("status")).isEqualTo("RESOLVED");
        assertThat(finalBetResponse.get("winnerId")).isEqualTo(tomUserId);
    }

    private String registerAndGetToken(String email, String username,
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

        Map<String, Object> response = parseResponse(result);
        return (String) response.get("accessToken");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseResponse(MvcResult result) throws Exception {
        return objectMapper.readValue(
                result.getResponse().getContentAsString(),
                Map.class
        );
    }
}
