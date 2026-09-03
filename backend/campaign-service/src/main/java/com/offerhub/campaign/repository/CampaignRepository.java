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

    /**
     * Both filters are optional; a null one means "any". A non null expertId narrows the
     * list to what that expert has a stake in - campaigns they created, plus campaigns
     * whose optimization case is assigned to them. Supervisors and admins pass null and
     * see everything, which is what the role matrix grants them.
     */
    @Query("""
            select c from Campaign c
            where (:status is null or c.status = :status)
              and (:segment is null or c.segment = :segment)
              and (:expertId is null
                   or c.createdBy = :expertId
                   or exists (select 1 from OptimizationCase oc
                              where oc.campaign = c and oc.assignedExpertId = :expertId))
            """)
    Page<Campaign> search(@Param("status") CampaignStatus status,
                          @Param("segment") Segment segment,
                          @Param("expertId") UUID expertId,
                          Pageable pageable);

    /**
     * Campaigns worth showing a subscriber: still valid and not retired. Matched on
     * targetSegment, the field that says who the campaign was aimed at - segment is what
     * it was classified as, which is a different question.
     * A null segment means the subscriber is not classified yet, and then nothing is
     * filtered out: we cannot claim a campaign is a poor fit for someone we do not know.
     */
    @Query("""
            select c from Campaign c
            where c.status <> com.offerhub.campaign.entity.CampaignStatus.ARSIVLENDI
              and c.validUntil > :now
              and (:segment is null or c.targetSegment = :segment)
            """)
    List<Campaign> findOfferable(@Param("segment") Segment segment, @Param("now") Instant now);

    /**
     * Case document 6.4, AI accuracy: a classification counts as correct while nobody has
     * corrected it, so accuracy is how many campaigns still carry the segment AI gave them.
     * BELIRSIZ is excluded from both sides - a campaign AI never managed to classify is not
     * evidence for or against its accuracy.
     */
    @Query("""
            select count(c) from Campaign c
            where c.aiSegment <> com.offerhub.campaign.entity.Segment.BELIRSIZ
            """)
    long countClassified();

    @Query("""
            select count(c) from Campaign c
            where c.aiSegment <> com.offerhub.campaign.entity.Segment.BELIRSIZ
              and c.segment = c.aiSegment
            """)
    long countClassifiedCorrectly();

    /** Dashboard: how campaigns are spread across segments. One row per segment in use. */
    @Query("select c.segment, count(c) from Campaign c group by c.segment")
    List<Object[]> countPerSegment();
}