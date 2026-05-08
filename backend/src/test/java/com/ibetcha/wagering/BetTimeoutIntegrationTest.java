package com.ibetcha.wagering;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibetcha.IntegrationTestBase;
import com.ibetcha.wagering.application.BetTimeoutScheduler;
import com.ibetcha.wagering.domain.Bet;
import com.ibetcha.wagering.infrastructure.BetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for bet timeout scheduler.
 *
 * Test budget: 2 behaviors × 2 = 4 tests max.
 * Behaviors:
 *   B1 — PENDING_ACCEPTANCE bet past acceptance_deadline → EXPIRED after scheduler runs.
 *   B2 — PENDING_JURY_VERDICT bet past jury_deadline → PENDING_APPROVAL after scheduler runs.
 *
 * The tests manipulate timestamps directly via the BetRepository to avoid waiting
 * for real timeouts in CI. The scheduler is invoked directly (not via @Scheduled trigger).
 */
class BetTimeoutIntegrationTest extends IntegrationTestBase {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BetRepository betRepository;

    @Autowired
    private BetTimeoutScheduler betTimeoutScheduler;

    private String tomToken;
    private String mariaToken;
    private String tomUserId;
    private String mariaUserId;
    private String alexToken;
    private String alexUserId;

    @BeforeEach
    void setupUsersAndFriendship() throws Exception {
        String suffix = String.valueOf(System.nanoTime());

        Map<String, Object> tomReg = registerUser("tom_timeout_" + suffix + "@example.com",
                "tom_t_" + suffix, "Str0ngPass1", "Tom Janssen");
        tomToken = (String) tomReg.get("accessToken");
        tomUserId = ((Map<String, Object>) tomReg.get("user")).get("id").toString();

        Map<String, Object> mariaReg = registerUser("maria_timeout_" + suffix + "@example.com",
                "maria_t_" + suffix, "Str0ngPass2", "Maria Santos");
        mariaToken = (String) mariaReg.get("accessToken");
        mariaUserId = ((Map<String, Object>) mariaReg.get("user")).get("id").toString();

        Map<String, Object> alexReg = registerUser("alex_timeout_" + suffix + "@example.com",
                "alex_t_" + suffix, "Str0ngPass3", "Alex Peeters");
        alexToken = (String) alexReg.get("accessToken");
        alexUserId = ((Map<String, Object>) alexReg.get("user")).get("id").toString();

        // Tom → Maria friend
        MvcResult req = mockMvc.perform(post("/api/v1/friends/request")
                        .header("Authorization", "Bearer " + tomToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetUserId": "%s"}
                                """.formatted(mariaUserId)))
                .andExpect(status().isCreated()).andReturn();
        String friendshipId = parseResponse(req).get("requestId").toString();
        mockMvc.perform(post("/api/v1/friends/" + friendshipId + "/accept")
                        .header("Authorization", "Bearer " + mariaToken))
                .andExpect(status().isOk());
    }

    @Test
    void pendingAcceptanceBet_pastDeadline_expiresOnSchedulerRun() throws Exception {
        // Create a bet (status = PENDING_ACCEPTANCE)
        MvcResult betResult = mockMvc.perform(post("/api/v1/bets")
                        .header("Authorization", "Bearer " + tomToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "description": "Timeout acceptance test",
                                    "stake": "Coffee",
                                    "participantIds": ["%s"]
                                }
                                """.formatted(mariaUserId)))
                .andExpect(status().isCreated())
                .andReturn();

        String betId = parseResponse(betResult).get("betId").toString();

        // Backdate the acceptance_deadline to the past via the repository
        Bet bet = betRepository.findById(java.util.UUID.fromString(betId)).orElseThrow();
        backdateAcceptanceDeadline(bet, Instant.now().minus(1, ChronoUnit.HOURS));

        // Run the scheduler
        betTimeoutScheduler.expireAcceptanceTimeouts();

        // Assert the bet is now EXPIRED
        MvcResult betAfter = mockMvc.perform(get("/api/v1/bets/" + betId)
                        .header("Authorization", "Bearer " + tomToken))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(parseResponse(betAfter).get("status")).isEqualTo("EXPIRED");
    }

    @Test
    void pendingJuryVerdictBet_pastJuryDeadline_escalatesToPendingApproval() throws Exception {
        // Create a bet with jury and activate it
        MvcResult betResult = mockMvc.perform(post("/api/v1/bets")
                        .header("Authorization", "Bearer " + tomToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "description": "Jury timeout test",
                                    "stake": "Dinner",
                                    "participantIds": ["%s"],
                                    "juryUserId": "%s"
                                }
                                """.formatted(mariaUserId, alexUserId)))
                .andExpect(status().isCreated())
                .andReturn();

        String betId = parseResponse(betResult).get("betId").toString();

        // Maria accepts to activate the bet
        mockMvc.perform(post("/api/v1/bets/" + betId + "/accept")
                        .header("Authorization", "Bearer " + mariaToken))
                .andExpect(status().isOk());

        // Tom claims victory (moves to PENDING_JURY_VERDICT)
        mockMvc.perform(post("/api/v1/bets/" + betId + "/complete")
                        .header("Authorization", "Bearer " + tomToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"winnerId": "%s"}
                                """.formatted(tomUserId)))
                .andExpect(status().isOk());

        // Verify it's in PENDING_JURY_VERDICT
        Map<String, Object> stateBeforeTimeout = parseResponse(mockMvc.perform(
                        get("/api/v1/bets/" + betId)
                                .header("Authorization", "Bearer " + tomToken))
                .andExpect(status().isOk()).andReturn());
        assertThat(stateBeforeTimeout.get("status")).isEqualTo("PENDING_JURY_VERDICT");

        // Backdate jury_deadline to the past
        Bet bet = betRepository.findById(java.util.UUID.fromString(betId)).orElseThrow();
        backdateJuryDeadline(bet, Instant.now().minus(1, ChronoUnit.HOURS));

        // Run the jury timeout scheduler
        betTimeoutScheduler.escalateJuryTimeouts();

        // Assert the bet is now PENDING_APPROVAL
        MvcResult betAfter = mockMvc.perform(get("/api/v1/bets/" + betId)
                        .header("Authorization", "Bearer " + tomToken))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(parseResponse(betAfter).get("status")).isEqualTo("PENDING_APPROVAL");
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Backdates acceptance_deadline by manipulating the JPA entity directly.
     * This avoids having to wait for real time to pass in tests.
     */
    private void backdateAcceptanceDeadline(Bet bet, Instant newDeadline) {
        // Use JPA native query via repository to set the deadline in the past
        betRepository.setAcceptanceDeadlineForTest(bet.getId(), newDeadline);
    }

    private void backdateJuryDeadline(Bet bet, Instant newDeadline) {
        betRepository.setJuryDeadlineForTest(bet.getId(), newDeadline);
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
