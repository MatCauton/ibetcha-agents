package com.ibetcha.identity.infrastructure;

import com.ibetcha.identity.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    Optional<User> findByAuthProviderAndProviderId(User.AuthProvider authProvider, String providerId);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    @Query("SELECT u FROM User u WHERE LOWER(u.username) LIKE LOWER(CONCAT(:prefix, '%')) " +
           "AND u.id != :excludeId AND u.accountStatus = :status ORDER BY u.username")
    List<User> searchByUsernamePrefix(@Param("prefix") String prefix, @Param("excludeId") UUID excludeId,
                                       @Param("status") User.AccountStatus status);

    default List<User> searchByUsernamePrefix(String prefix, UUID excludeId) {
        return searchByUsernamePrefix(prefix, excludeId, User.AccountStatus.ACTIVE);
    }
}
