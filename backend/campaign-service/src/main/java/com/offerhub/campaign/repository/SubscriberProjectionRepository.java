package com.offerhub.campaign.repository;

import com.offerhub.campaign.entity.SubscriberProjection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SubscriberProjectionRepository extends JpaRepository<SubscriberProjection, UUID> {
}
