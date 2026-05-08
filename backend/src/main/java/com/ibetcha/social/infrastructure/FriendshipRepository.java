package com.ibetcha.social.infrastructure;

import com.ibetcha.social.domain.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FriendshipRepository extends JpaRepository<Friendship, UUID> {

    @Query("SELECT f FROM Friendship f WHERE " +
           "((f.userIdLower = :userA AND f.userIdHigher = :userB) " +
           "OR (f.userIdLower = :userB AND f.userIdHigher = :userA)) " +
           "AND f.status != 'REMOVED'")
    Optional<Friendship> findByUserPair(@Param("userA") UUID userA, @Param("userB") UUID userB);

    @Query("SELECT f FROM Friendship f WHERE " +
           "(f.userIdLower = :userId OR f.userIdHigher = :userId) " +
           "AND f.status = 'ACTIVE'")
    List<Friendship> findActiveFriendships(@Param("userId") UUID userId);

    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Friendship f WHERE " +
           "f.userIdLower = LEAST(:userA, :userB) AND f.userIdHigher = GREATEST(:userA, :userB) " +
           "AND f.status = 'ACTIVE'")
    boolean areFriends(@Param("userA") UUID userA, @Param("userB") UUID userB);

    @Query("SELECT f FROM Friendship f WHERE " +
           "(f.userIdLower = :userId OR f.userIdHigher = :userId) " +
           "AND f.status = 'PENDING' AND f.requesterId != :userId")
    List<Friendship> findPendingRequestsForUser(@Param("userId") UUID userId);
}
