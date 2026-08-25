package com.offerhub.campaign.repository;

import com.offerhub.campaign.entity.CampaignCounter;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface CampaignCounterRepository extends JpaRepository<CampaignCounter, Integer> {

    /**
     * SELECT ... FOR UPDATE: two concurrent campaign creations cannot read the same
     * counter value, so they cannot end up with the same campaign number.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<CampaignCounter> findByYear(Integer year);
}