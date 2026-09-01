package com.offerhub.gamification.repository;

import com.offerhub.gamification.entity.ExpertProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ExpertProfileRepository extends JpaRepository<ExpertProfile, UUID> {
}
