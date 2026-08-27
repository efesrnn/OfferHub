package com.offerhub.campaign.repository;

import com.offerhub.campaign.entity.OptimizationCase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OptimizationCaseRepository extends JpaRepository<OptimizationCase, UUID> {
}
