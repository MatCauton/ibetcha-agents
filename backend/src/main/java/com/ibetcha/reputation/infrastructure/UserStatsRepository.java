package com.ibetcha.reputation.infrastructure;

import com.ibetcha.reputation.domain.UserStats;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserStatsRepository extends JpaRepository<UserStats, UUID> {
}
