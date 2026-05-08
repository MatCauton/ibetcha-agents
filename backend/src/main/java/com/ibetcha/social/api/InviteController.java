package com.ibetcha.social.api;

import com.ibetcha.shared.security.AuthenticatedUser;
import com.ibetcha.social.application.InviteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/invites")
public class InviteController {

    private final InviteService inviteService;

    public InviteController(InviteService inviteService) {
        this.inviteService = inviteService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createOrGetInvite() {
        UUID inviterId = AuthenticatedUser.currentUserId();
        InviteService.InviteResult result = inviteService.getOrCreateInvite(inviterId);
        return ResponseEntity.ok(Map.of(
                "code", result.code(),
                "inviteUrl", result.inviteUrl(),
                "inviterId", result.inviterId()
        ));
    }

    @GetMapping("/{code}")
    public ResponseEntity<Map<String, Object>> resolveInvite(@PathVariable String code) {
        InviteService.InvitePreview preview = inviteService.resolveCode(code);
        Map<String, Object> body = new HashMap<>();
        body.put("code", preview.code());
        body.put("inviterDisplayName", preview.inviterDisplayName());
        body.put("inviterUsername", preview.inviterUsername());
        return ResponseEntity.ok(body);
    }

    @PostMapping("/{code}/accept")
    public ResponseEntity<Map<String, Object>> acceptInvite(@PathVariable String code) {
        UUID accepterId = AuthenticatedUser.currentUserId();
        InviteService.AcceptResult result = inviteService.acceptInvite(code, accepterId);
        return ResponseEntity.ok(Map.of(
                "requestSent", result.requestSent(),
                "alreadyFriends", result.alreadyFriends()
        ));
    }
}
