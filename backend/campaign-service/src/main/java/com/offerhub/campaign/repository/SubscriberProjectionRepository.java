package com.offerhub.campaign.repository;

import com.offerhub.campaign.entity.Segment;
import com.offerhub.campaign.entity.SubscriberProjection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SubscriberProjectionRepository extends JpaRepository<SubscriberProjection, UUID> {

    /** A sample of the subscribers in one segment; Pageable caps how many come back. */
    List<SubscriberProjection> findBySegment(Segment segment, Pageable pageable);
}
