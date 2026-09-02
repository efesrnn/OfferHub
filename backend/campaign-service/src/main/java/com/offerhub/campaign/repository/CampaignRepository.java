package com.offerhub.campaign.repository;

import com.offerhub.campaign.entity.Campaign;
import com.offerhub.campaign.entity.CampaignStatus;
import com.offerhub.campaign.entity.Segment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CampaignRepository extends JpaRepository<Campaign, UUID> {

    /** API paths address campaigns by campaignNo, not by the internal UUID. */
    Optional<Campaign> findByCampaignNo(String campaignNo);

    /** Abonelere teklif olarak gosterilebilecek kampanyalar: yayinda ve suresi dolmamis. */
    List<Campaign> findByStatusAndValidUntilAfter(CampaignStatus status, Instant now);

    /** Both filters are optional; a null one means "any". */
    @Query("""
            select c from Campaign c
            where (:status is null or c.status = :status)
              and (:segment is null or c.segment = :segment)
            """)
    Page<Campaign> search(@Param("status") CampaignStatus status,
                          @Param("segment") Segment segment,
                          Pageable pageable);
}