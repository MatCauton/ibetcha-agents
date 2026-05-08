package com.ibetcha.social.application;

import com.ibetcha.identity.domain.User;
import com.ibetcha.identity.infrastructure.UserRepository;
import com.ibetcha.shared.exception.ApiException;
import com.ibetcha.social.domain.Friendship;
import com.ibetcha.social.domain.InviteLink;
import com.ibetcha.social.infrastructure.FriendshipRepository;
import com.ibetcha.social.infrastructure.InviteLinkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Optional;
import java.util.UUID;

@Service
public class InviteService {

    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final InviteLinkRepository inviteLinkRepository;
    private final UserRepository userRepository;
    private final FriendshipService friendshipService;
    private final FriendshipRepository friendshipRepository;

    public InviteService(InviteLinkRepository inviteLinkRepository,
                         UserRepository userRepository,
                         FriendshipService friendshipService,
                         FriendshipRepository friendshipRepository) {
        this.inviteLinkRepository = inviteLinkRepository;
        this.userRepository = userRepository;
        this.friendshipService = friendshipService;
        this.friendshipRepository = friendshipRepository;
    }

    @Transactional
    public InviteResult getOrCreateInvite(UUID inviterId) {
        return inviteLinkRepository.findByInviterId(inviterId)
                .map(link -> new InviteResult(link.getReferralCode(), inviterId))
                .orElseGet(() -> {
                    String code = generateUniqueCode();
                    InviteLink link = InviteLink.create(inviterId, code);
                    inviteLinkRepository.save(link);
                    return new InviteResult(code, inviterId);
                });
    }

    @Transactional(readOnly = true)
    public InvitePreview resolveCode(String code) {
        InviteLink link = inviteLinkRepository.findByReferralCode(code)
                .orElseThrow(() -> ApiException.notFound("Invite code not found"));

        User inviter = userRepository.findById(link.getInviterId())
                .orElseThrow(() -> ApiException.notFound("Inviter not found"));

        String displayName = inviter.getDisplayName() != null ? inviter.getDisplayName() : inviter.getUsername();
        return new InvitePreview(code, displayName, inviter.getUsername());
    }

    @Transactional
    public AcceptResult acceptInvite(String code, UUID accepterId) {
        InviteLink link = inviteLinkRepository.findByReferralCode(code)
                .orElseThrow(() -> ApiException.notFound("Invite code not found"));

        UUID inviterId = link.getInviterId();

        if (inviterId.equals(accepterId)) {
            throw ApiException.badRequest("Cannot accept your own invite");
        }

        Optional<Friendship> existing = friendshipRepository.findByUserPair(accepterId, inviterId);
        if (existing.isPresent()) {
            Friendship f = existing.get();
            if (f.getStatus() == Friendship.FriendshipStatus.ACTIVE
                    || f.getStatus() == Friendship.FriendshipStatus.PENDING) {
                return new AcceptResult(false, true);
            }
        }

        friendshipService.sendRequest(accepterId, inviterId);
        return new AcceptResult(true, false);
    }

    private String generateUniqueCode() {
        String code = randomCode();
        if (inviteLinkRepository.findByReferralCode(code).isPresent()) {
            code = randomCode();
        }
        return code;
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    public record InviteResult(String code, UUID inviterId) {
        public String inviteUrl() {
            return "ibetcha://invite/" + code;
        }
    }

    public record InvitePreview(String code, String inviterDisplayName, String inviterUsername) {}

    public record AcceptResult(boolean requestSent, boolean alreadyFriends) {}
}
