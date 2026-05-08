package com.ibetcha.social.api;

import com.ibetcha.shared.security.AuthenticatedUser;
import com.ibetcha.social.application.FriendshipService;
import com.ibetcha.social.domain.Friendship;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/friends")
public class FriendController {

    private final FriendshipService friendshipService;

    public FriendController(FriendshipService friendshipService) {
        this.friendshipService = friendshipService;
    }

    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchUsers(@RequestParam("q") String query) {
        UUID currentUserId = AuthenticatedUser.currentUserId();
        List<FriendshipService.UserSearchResult> results = friendshipService.searchUsers(query, currentUserId);
        return ResponseEntity.ok(Map.of("results", results));
    }

    @PostMapping("/request")
    public ResponseEntity<Map<String, Object>> sendRequest(@Valid @RequestBody FriendRequest request) {
        UUID currentUserId = AuthenticatedUser.currentUserId();
        Friendship friendship = friendshipService.sendRequest(currentUserId, request.targetUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "requestId", friendship.getId(),
                "status", friendship.getStatus().name()
        ));
    }

    @PostMapping("/{friendshipId}/accept")
    public ResponseEntity<Map<String, Object>> acceptRequest(@PathVariable UUID friendshipId) {
        UUID currentUserId = AuthenticatedUser.currentUserId();
        Friendship friendship = friendshipService.acceptRequest(friendshipId, currentUserId);
        UUID friendId = friendship.getOtherUser(currentUserId);
        return ResponseEntity.ok(Map.of(
                "friendshipId", friendship.getId(),
                "friendUserId", friendId
        ));
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> listFriends() {
        UUID currentUserId = AuthenticatedUser.currentUserId();
        List<FriendshipService.FriendInfo> friends = friendshipService.listFriends(currentUserId);
        return ResponseEntity.ok(Map.of("friends", friends));
    }

    public record FriendRequest(@NotNull UUID targetUserId) {}
}
