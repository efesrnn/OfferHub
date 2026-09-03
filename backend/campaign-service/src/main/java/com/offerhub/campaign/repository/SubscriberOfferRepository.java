package com.offerhub.campaign.repository;

import com.offerhub.campaign.entity.SubscriberOffer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriberOfferRepository extends JpaRepository<SubscriberOffer, UUID> {

    List<SubscriberOffer> findBySubscriberIdOrderByCreatedAtDesc(UUID subscriberId);

    Optional<SubscriberOffer> findByIdAndSubscriberId(UUID id, UUID subscriberId);
}
