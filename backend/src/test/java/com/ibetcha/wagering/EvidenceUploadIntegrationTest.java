package com.ibetcha.wagering;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibetcha.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EvidenceUploadIntegrationTest extends IntegrationTestBase {

    @Autowired
    private ObjectMapper objectMapper;

    private String tomToken;
    private String mariaToken;
    private String outsiderToken;
    private String tomUserId;
    private String mariaUserId;

    @BeforeEach
    void setupUsersAndFriendship() throws Exception {
        String suffix = String.valueOf(System.nanoTime());

        Map<String, Object> tomReg = registerUser(
                "ev_tom_" + suffix + "@example.com",
                "ev_tom_" + suffix,
                "Str0ngPass1",
                "Tom Evidence"
        );
        tomToken = (String) tomReg.get("accessToken");
        tomUserId = ((Map<String, Object>) tomReg.get("user")).get("id").toString();

        Map<String, Object> mariaReg = registerUser(
                "ev_maria_" + suffix + "@example.com",
                "ev_maria_" + suffix,
                "Str0ngPass2",
                "Maria Evidence"
        );
        mariaToken = (String) mariaReg.get("accessToken");
        mariaUserId = ((Map<String, Object>) mariaReg.get("user")).get("id").toString();

        Map<String, Object> outsiderReg = registerUser(
                "ev_outsider_" + suffix + "@example.com",
                "ev_outsider_" + suffix,
                "Str0ngPass3",
                "Outsider"
        );
        outsiderToken = (String) outsiderReg.get("accessToken");

        // Tom and Maria become friends
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
    void participantCanGetPresignedUrlAndRegisterEvidence() throws Exception {
        String betId = createAndActivateBet();

        // Get presigned upload URL as participant (Tom)
        MvcResult urlResult = mockMvc.perform(get("/api/v1/bets/" + betId + "/evidence/upload-url")
                        .header("Authorization", "Bearer " + tomToken)
                        .param("contentType", "image/jpeg"))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> urlResponse = parseResponse(urlResult);
        assertThat(urlResponse.get("uploadUrl").toString()).startsWith("https://mock-s3.local/");
        assertThat(urlResponse.get("s3Key").toString()).startsWith("evidence/" + betId + "/");
        assertThat(urlResponse.get("contentType")).isEqualTo("image/jpeg");
        assertThat(urlResponse.get("expiresIn")).isEqualTo(900);

        String s3Key = urlResponse.get("s3Key").toString();

        // Register evidence after upload
        MvcResult evidenceResult = mockMvc.perform(post("/api/v1/bets/" + betId + "/evidence")
                        .header("Authorization", "Bearer " + tomToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "s3Key": "%s",
                                    "contentType": "image/jpeg",
                                    "fileName": "proof.jpg"
                                }
                                """.formatted(s3Key)))
                .andExpect(status().isCreated())
                .andReturn();

        Map<String, Object> evidenceResponse = parseResponse(evidenceResult);
        assertThat(evidenceResponse.get("evidenceId")).isNotNull();
        assertThat(evidenceResponse.get("s3Key")).isEqualTo(s3Key);
        assertThat(evidenceResponse.get("contentType")).isEqualTo("image/jpeg");
        assertThat(evidenceResponse.get("fileName")).isEqualTo("proof.jpg");
        assertThat(evidenceResponse.get("uploadedAt")).isNotNull();
    }

    @Test
    void nonParticipantCannotUpload() throws Exception {
        String betId = createAndActivateBet();

        mockMvc.perform(get("/api/v1/bets/" + betId + "/evidence/upload-url")
                        .header("Authorization", "Bearer " + outsiderToken)
                        .param("contentType", "image/jpeg"))
                .andExpect(status().isForbidden());
    }

    @Test
    void uploadToInactiveBet_returns409() throws Exception {
        // Create bet but do NOT activate it (stays PENDING_ACCEPTANCE)
        MvcResult betResult = mockMvc.perform(post("/api/v1/bets")
                        .header("Authorization", "Bearer " + tomToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "description": "Evidence inactive bet",
                                    "stake": "Coffee",
                                    "participantIds": ["%s"]
                                }
                                """.formatted(mariaUserId)))
                .andExpect(status().isCreated())
                .andReturn();
        String betId = parseResponse(betResult).get("betId").toString();

        mockMvc.perform(get("/api/v1/bets/" + betId + "/evidence/upload-url")
                        .header("Authorization", "Bearer " + tomToken)
                        .param("contentType", "image/jpeg"))
                .andExpect(status().isConflict());
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private String createAndActivateBet() throws Exception {
        MvcResult betResult = mockMvc.perform(post("/api/v1/bets")
                        .header("Authorization", "Bearer " + tomToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "description": "Evidence test bet",
                                    "stake": "Bragging rights",
                                    "participantIds": ["%s"]
                                }
                                """.formatted(mariaUserId)))
                .andExpect(status().isCreated())
                .andReturn();
        String betId = parseResponse(betResult).get("betId").toString();

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
