package com.offerhub.campaign.repository;

import com.offerhub.campaign.entity.Campaign;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CampaignRepository extends JpaRepository<Campaign, UUID> {

    /** API paths address campaigns by campaignNo, not by the internal UUID. */
    Optional<Campaign> findByCampaignNo(String campaignNo);
}