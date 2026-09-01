package com.offerhub.gamification.repository;

import com.offerhub.gamification.entity.Badge;
import com.offerhub.gamification.entity.EarnedBadge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EarnedBadgeRepository extends JpaRepository<EarnedBadge, UUID> {

    List<EarnedBadge> findByExpertIdOrderByEarnedAtAsc(UUID expertId);

    boolean existsByExpertIdAndBadge(UUID expertId, Badge badge);
}
