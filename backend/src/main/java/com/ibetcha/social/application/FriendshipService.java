package com.ibetcha.social.application;

import com.ibetcha.identity.domain.User;
import com.ibetcha.identity.infrastructure.UserRepository;
import com.ibetcha.shared.exception.ApiException;
import com.ibetcha.social.domain.Friendship;
import com.ibetcha.social.infrastructure.FriendshipRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class FriendshipService {

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;

    public FriendshipService(FriendshipRepository friendshipRepository,
                             UserRepository userRepository) {
        this.friendshipRepository = friendshipRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Friendship sendRequest(UUID requesterId, UUID targetUserId) {
        if (requesterId.equals(targetUserId)) {
            throw ApiException.badRequest("Cannot send a friend request to yourself");
        }

        userRepository.findById(targetUserId)
                .orElseThrow(() -> ApiException.notFound("User not found"));

        Optional<Friendship> existing = friendshipRepository.findByUserPair(requesterId, targetUserId);
        if (existing.isPresent()) {
            Friendship f = existing.get();
            if (f.getStatus() == Friendship.FriendshipStatus.ACTIVE) {
                throw ApiException.conflict("Already friends");
            }
            if (f.getStatus() == Friendship.FriendshipStatus.PENDING) {
                throw ApiException.conflict("Friend request already pending");
            }
        }

        Friendship friendship = Friendship.createRequest(requesterId, targetUserId);
        return friendshipRepository.save(friendship);
    }

    @Transactional
    public Friendship acceptRequest(UUID friendshipId, UUID userId) {
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> ApiException.notFound("Friend request not found"));

        if (!friendship.involves(userId) || friendship.getRequesterId().equals(userId)) {
            throw ApiException.forbidden("Cannot accept your own request");
        }

        friendship.accept();
        return friendshipRepository.save(friendship);
    }

    @Transactional(readOnly = true)
    public List<FriendInfo> listFriends(UUID userId) {
        List<Friendship> friendships = friendshipRepository.findActiveFriendships(userId);
        List<FriendInfo> friends = new ArrayList<>();

        for (Friendship f : friendships) {
            UUID friendId = f.getOtherUser(userId);
            userRepository.findById(friendId).ifPresent(user ->
                    friends.add(new FriendInfo(
                            user.getId(),
                            user.getUsername(),
                            user.getDisplayName(),
                            user.getAvatarUrl()
                    ))
            );
        }
        return friends;
    }

    @Transactional(readOnly = true)
    public List<UserSearchResult> searchUsers(String query, UUID currentUserId) {
        List<User> users = userRepository.searchByUsernamePrefix(query, currentUserId);
        List<UserSearchResult> results = new ArrayList<>();

        for (User user : users) {
            String friendshipStatus = "NONE";
            Optional<Friendship> friendship = friendshipRepository.findByUserPair(currentUserId, user.getId());
            if (friendship.isPresent()) {
                Friendship f = friendship.get();
                if (f.getStatus() == Friendship.FriendshipStatus.ACTIVE) {
                    friendshipStatus = "FRIENDS";
                } else if (f.getStatus() == Friendship.FriendshipStatus.PENDING) {
                    friendshipStatus = f.getRequesterId().equals(currentUserId) ? "PENDING_SENT" : "PENDING_RECEIVED";
                }
            }
            results.add(new UserSearchResult(
                    user.getId(), user.getUsername(), user.getDisplayName(),
                    user.getAvatarUrl(), friendshipStatus
            ));
        }
        return results;
    }

    public boolean areFriends(UUID userA, UUID userB) {
        return friendshipRepository.areFriends(userA, userB);
    }

    public record FriendInfo(UUID userId, String username, String displayName, String avatarUrl) {}

    public record UserSearchResult(UUID userId, String username, String displayName,
                                   String avatarUrl, String friendshipStatus) {}
}
