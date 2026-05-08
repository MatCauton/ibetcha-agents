package com.ibetcha.social;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibetcha.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InviteIntegrationTest extends IntegrationTestBase {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createInvite_returnsCode_andSecondCallReturnsSameCode() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        Map<String, Object> reg = registerUser(
                "inviter_" + suffix + "@example.com",
                "inviter_" + suffix,
                "Str0ngPass1",
                "Invite User"
        );
        String token = (String) reg.get("accessToken");

        MvcResult first = mockMvc.perform(post("/api/v1/invites")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> firstBody = parseResponse(first);
        String code = (String) firstBody.get("code");
        assertThat(code).hasSize(8);
        assertThat(firstBody.get("inviteUrl")).isEqualTo("ibetcha://invite/" + code);

        MvcResult second = mockMvc.perform(post("/api/v1/invites")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(parseResponse(second).get("code")).isEqualTo(code);
    }

    @Test
    void resolveInvite_returnsInviterInfo_withoutAuth() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        Map<String, Object> reg = registerUser(
                "tom_" + suffix + "@example.com",
                "tom_" + suffix,
                "Str0ngPass1",
                "Tom Hanks"
        );
        String token = (String) reg.get("accessToken");

        MvcResult createResult = mockMvc.perform(post("/api/v1/invites")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        String code = (String) parseResponse(createResult).get("code");

        MvcResult resolveResult = mockMvc.perform(get("/api/v1/invites/" + code))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> body = parseResponse(resolveResult);
        assertThat(body.get("code")).isEqualTo(code);
        assertThat(body.get("inviterDisplayName")).isEqualTo("Tom Hanks");
        assertThat(body.get("inviterUsername")).isEqualTo("tom_" + suffix);
    }

    @Test
    void acceptInvite_sendsFriendRequest() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        Map<String, Object> inviterReg = registerUser(
                "inviterA_" + suffix + "@example.com",
                "inviterA_" + suffix,
                "Str0ngPass1",
                "Alice"
        );
        String inviterToken = (String) inviterReg.get("accessToken");

        Map<String, Object> accepterReg = registerUser(
                "accepterB_" + suffix + "@example.com",
                "accepterB_" + suffix,
                "Str0ngPass2",
                "Bob"
        );
        String accepterToken = (String) accepterReg.get("accessToken");

        MvcResult createResult = mockMvc.perform(post("/api/v1/invites")
                        .header("Authorization", "Bearer " + inviterToken))
                .andExpect(status().isOk())
                .andReturn();
        String code = (String) parseResponse(createResult).get("code");

        MvcResult acceptResult = mockMvc.perform(post("/api/v1/invites/" + code + "/accept")
                        .header("Authorization", "Bearer " + accepterToken))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> body = parseResponse(acceptResult);
        assertThat(body.get("requestSent")).isEqualTo(true);
        assertThat(body.get("alreadyFriends")).isEqualTo(false);
    }

    @Test
    void acceptOwnInvite_returns400() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        Map<String, Object> reg = registerUser(
                "selfie_" + suffix + "@example.com",
                "selfie_" + suffix,
                "Str0ngPass1",
                "Selfie User"
        );
        String token = (String) reg.get("accessToken");

        MvcResult createResult = mockMvc.perform(post("/api/v1/invites")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        String code = (String) parseResponse(createResult).get("code");

        mockMvc.perform(post("/api/v1/invites/" + code + "/accept")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseResponse(MvcResult result) throws Exception {
        return objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
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
}
