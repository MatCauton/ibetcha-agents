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

class WinCardIntegrationTest extends IntegrationTestBase {

    @Autowired
    private ObjectMapper objectMapper;

    private String tomToken;
    private String mariaToken;
    private String tomUserId;
    private String mariaUserId;

    @BeforeEach
    void setupUsersAndFriendship() throws Exception {
        String suffix = String.valueOf(System.nanoTime());

        Map<String, Object> tomReg = registerUser(
                "wc_tom_" + suffix + "@example.com",
                "wc_tom_" + suffix,
                "Str0ngPass1",
                "Tom WinCard"
        );
        tomToken = (String) tomReg.get("accessToken");
        tomUserId = ((Map<String, Object>) tomReg.get("user")).get("id").toString();

        Map<String, Object> mariaReg = registerUser(
                "wc_maria_" + suffix + "@example.com",
                "wc_maria_" + suffix,
                "Str0ngPass2",
                "Maria WinCard"
        );
        mariaToken = (String) mariaReg.get("accessToken");
        mariaUserId = ((Map<String, Object>) mariaReg.get("user")).get("id").toString();

        // Friendship
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
    @SuppressWarnings("unchecked")
    void resolvedBetReturnsWinCard() throws Exception {
        // Tom concedes to Maria (resolves immediately)
        MvcResult betResult = mockMvc.perform(post("/api/v1/bets")
                        .header("Authorization", "Bearer " + tomToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "description": "Sunday cycling race challenge",
                                    "stake": "Loser buys coffee",
                                    "participantIds": ["%s"]
                                }
                                """.formatted(mariaUserId)))
                .andExpect(status().isCreated())
                .andReturn();
        String betId = parseResponse(betResult).get("betId").toString();

        mockMvc.perform(post("/api/v1/bets/" + betId + "/accept")
                        .header("Authorization", "Bearer " + mariaToken))
                .andExpect(status().isOk());

        // Tom concedes — Maria wins
        mockMvc.perform(post("/api/v1/bets/" + betId + "/complete")
                        .header("Authorization", "Bearer " + tomToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"winnerId": "%s"}
                                """.formatted(mariaUserId)))
                .andExpect(status().isOk());

        // Fetch Win Card
        MvcResult winCardResult = mockMvc.perform(get("/api/v1/bets/" + betId + "/win-card")
                        .header("Authorization", "Bearer " + tomToken))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> wc = parseResponse(winCardResult);
        assertThat(wc.get("betId")).isEqualTo(betId);
        assertThat(wc.get("description")).isEqualTo("Sunday cycling race challenge");
        assertThat(wc.get("stake")).isEqualTo("Loser buys coffee");
        assertThat(wc.get("resolvedAt")).isNotNull();

        Map<String, Object> winner = (Map<String, Object>) wc.get("winner");
        assertThat(winner.get("userId")).isEqualTo(mariaUserId);
        assertThat(winner.get("displayName")).isEqualTo("Maria WinCard");

        Map<String, Object> loser = (Map<String, Object>) wc.get("loser");
        assertThat(loser.get("userId")).isEqualTo(tomUserId);

        List<Map<String, Object>> allParticipants = (List<Map<String, Object>>) wc.get("allParticipants");
        assertThat(allParticipants).hasSize(2);

        Map<String, Object> h2h = (Map<String, Object>) wc.get("headToHeadRecord");
        assertThat(h2h.get("wins")).isEqualTo(1);
        assertThat(h2h.get("losses")).isEqualTo(0);
    }

    @Test
    void unresolvedBet_returns409() throws Exception {
        MvcResult betResult = mockMvc.perform(post("/api/v1/bets")
                        .header("Authorization", "Bearer " + tomToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "description": "Unresolved bet",
                                    "stake": "Coffee",
                                    "participantIds": ["%s"]
                                }
                                """.formatted(mariaUserId)))
                .andExpect(status().isCreated())
                .andReturn();
        String betId = parseResponse(betResult).get("betId").toString();

        // Bet is still PENDING_ACCEPTANCE — not resolved
        mockMvc.perform(get("/api/v1/bets/" + betId + "/win-card")
                        .header("Authorization", "Bearer " + tomToken))
                .andExpect(status().isConflict());
    }

    // ── Helpers ──────────────────────────────────────────────────────────

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
