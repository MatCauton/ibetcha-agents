package com.ibetcha.shared.dev;

import com.ibetcha.identity.domain.User;
import com.ibetcha.identity.infrastructure.UserRepository;
import com.ibetcha.reputation.application.StatsService;
import com.ibetcha.social.domain.Friendship;
import com.ibetcha.social.infrastructure.FriendshipRepository;
import com.ibetcha.wagering.domain.Bet;
import com.ibetcha.wagering.domain.BetParticipant;
import com.ibetcha.wagering.domain.OutcomeClaim;
import com.ibetcha.wagering.infrastructure.BetParticipantRepository;
import com.ibetcha.wagering.infrastructure.BetRepository;
import com.ibetcha.wagering.infrastructure.OutcomeClaimRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Seeds dev accounts, friendships, and sample bets on every non-prod startup.
 * Idempotent: skips creation if data already exists.
 *
 * Accounts (all password: "password"):
 *   alice@ibetcha.test   — Alice
 *   bob@ibetcha.test     — Bob
 *   charlie@ibetcha.test — Charlie (jury user)
 */
@Component
@Profile("!prod")
public class DevDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final FriendshipRepository friendshipRepository;
    private final BetRepository betRepository;
    private final BetParticipantRepository participantRepository;
    private final OutcomeClaimRepository claimRepository;
    private final StatsService statsService;

    public DevDataSeeder(UserRepository userRepository,
                         PasswordEncoder passwordEncoder,
                         FriendshipRepository friendshipRepository,
                         BetRepository betRepository,
                         BetParticipantRepository participantRepository,
                         OutcomeClaimRepository claimRepository,
                         StatsService statsService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.friendshipRepository = friendshipRepository;
        this.betRepository = betRepository;
        this.participantRepository = participantRepository;
        this.claimRepository = claimRepository;
        this.statsService = statsService;
    }

    @Override
    public void run(String... args) {
        User alice   = createUserIfAbsent("alice@ibetcha.test",   "alice",   "Alice");
        User bob     = createUserIfAbsent("bob@ibetcha.test",     "bob",     "Bob");
        User charlie = createUserIfAbsent("charlie@ibetcha.test", "charlie", "Charlie");

        createFriendshipIfAbsent(alice, bob);
        createFriendshipIfAbsent(alice, charlie);
        createFriendshipIfAbsent(bob, charlie);

        if (betRepository.count() == 0) {
            seedBets(alice, bob, charlie);
            log.info("[DEV] Sample bets created");
        }
    }

    // ── Users ─────────────────────────────────────────────────────────────────

    private User createUserIfAbsent(String email, String username, String displayName) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            User user = User.createEmailUser(
                    UUID.randomUUID(), email, username, displayName,
                    passwordEncoder.encode("password"));
            userRepository.save(user);
            log.info("[DEV] Created user: {} / password", email);
            return user;
        });
    }

    // ── Friendships ───────────────────────────────────────────────────────────

    private void createFriendshipIfAbsent(User a, User b) {
        if (friendshipRepository.findByUserPair(a.getId(), b.getId()).isPresent()) return;
        Friendship f = Friendship.createRequest(a.getId(), b.getId());
        f.accept();
        friendshipRepository.save(f);
        log.info("[DEV] Friendship: {} <-> {}", a.getUsername(), b.getUsername());
    }

    // ── Bets ──────────────────────────────────────────────────────────────────

    private void seedBets(User alice, User bob, User charlie) {
        createPendingAcceptanceBet(alice, bob);
        createActiveBet(alice, bob);
        createPendingApprovalBet(bob, alice);
        createPendingJuryVerdictBet(alice, bob, charlie);
        createResolvedBet(alice, bob);
        createExpiredBet(bob, alice);
    }

    /** Alice challenged Bob — Bob hasn't accepted yet. */
    private void createPendingAcceptanceBet(User alice, User bob) {
        Bet bet = Bet.create(alice.getId(),
                "I'll finish the project report before you finish yours",
                "Loser buys coffee for a week",
                "Report race", null,
                Instant.now().plus(14, ChronoUnit.DAYS), false);
        betRepository.save(bet);

        participantRepository.save(BetParticipant.createCreator(bet.getId(), alice.getId()));
        participantRepository.save(BetParticipant.createInvitee(bet.getId(), bob.getId()));
    }

    /** Both accepted — bet is in progress. */
    private void createActiveBet(User alice, User bob) {
        Bet bet = Bet.create(bob.getId(),
                "Who scores more points in our next board game night?",
                "Winner picks the next game",
                "Board game champion", null,
                Instant.now().plus(7, ChronoUnit.DAYS), false);
        bet.activate();
        betRepository.save(bet);

        BetParticipant creator = BetParticipant.createCreator(bet.getId(), bob.getId());
        BetParticipant invitee = BetParticipant.createInvitee(bet.getId(), alice.getId());
        invitee.accept();
        participantRepository.save(creator);
        participantRepository.save(invitee);
    }

    /** Bob declared Alice as winner — Alice needs to approve. */
    private void createPendingApprovalBet(User bob, User alice) {
        Bet bet = Bet.create(bob.getId(),
                "First one to run a 5K without stopping",
                "Winner gets bragging rights for a month",
                "5K challenge", null,
                Instant.now().plus(30, ChronoUnit.DAYS), true);
        bet.activate();
        bet.moveToPendingApproval();
        betRepository.save(bet);

        BetParticipant creator = BetParticipant.createCreator(bet.getId(), bob.getId());
        BetParticipant invitee = BetParticipant.createInvitee(bet.getId(), alice.getId());
        invitee.accept();
        participantRepository.save(creator);
        participantRepository.save(invitee);

        // Bob declared Alice as the winner
        OutcomeClaim claim = OutcomeClaim.create(bet.getId(), bob.getId(), alice.getId());
        claimRepository.save(claim);
    }

    /** Alice challenged Bob, Charlie is jury. Outcome sent to jury. */
    private void createPendingJuryVerdictBet(User alice, User bob, User charlie) {
        Bet bet = Bet.create(alice.getId(),
                "Best karaoke performance at the party",
                "Loser has to do a solo encore",
                "Karaoke king", charlie.getId(),
                Instant.now().plus(3, ChronoUnit.DAYS), false);
        bet.activate();
        bet.moveToPendingJuryVerdict();
        betRepository.save(bet);

        BetParticipant creator = BetParticipant.createCreator(bet.getId(), alice.getId());
        BetParticipant invitee = BetParticipant.createInvitee(bet.getId(), bob.getId());
        invitee.accept();
        participantRepository.save(creator);
        participantRepository.save(invitee);

        // Alice declared herself the winner — now waiting on Charlie's verdict
        OutcomeClaim claim = OutcomeClaim.create(bet.getId(), alice.getId(), alice.getId());
        claimRepository.save(claim);
    }

    /** Resolved: Alice won, Bob conceded. Stats updated. */
    private void createResolvedBet(User alice, User bob) {
        Bet bet = Bet.create(alice.getId(),
                "I can name every country in Europe in under 2 minutes",
                "Loser buys pizza",
                "Geography quiz", null, null, false);
        bet.activate();
        bet.resolve(alice.getId());
        betRepository.save(bet);

        participantRepository.save(BetParticipant.createCreator(bet.getId(), alice.getId()));
        BetParticipant invitee = BetParticipant.createInvitee(bet.getId(), bob.getId());
        invitee.accept();
        participantRepository.save(invitee);

        // Update reputation stats
        statsService.updateOnBetResolved(alice.getId(), List.of(bob.getId()));
    }

    /** Expired: acceptance deadline already passed. */
    private void createExpiredBet(User bob, User alice) {
        Bet bet = Bet.create(bob.getId(),
                "I'll finish reading War and Peace this month",
                "Buys the next round",
                "Reading bet", null, null, false);
        bet.expire();
        betRepository.save(bet);

        participantRepository.save(BetParticipant.createCreator(bet.getId(), bob.getId()));
        participantRepository.save(BetParticipant.createInvitee(bet.getId(), alice.getId()));
    }
}
