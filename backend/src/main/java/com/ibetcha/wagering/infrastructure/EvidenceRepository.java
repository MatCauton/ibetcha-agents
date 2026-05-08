package com.ibetcha.wagering.infrastructure;

import com.ibetcha.wagering.domain.Evidence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EvidenceRepository extends JpaRepository<Evidence, UUID> {

    List<Evidence> findByBetIdOrderByUploadedAtDesc(UUID betId);
}
