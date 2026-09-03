package com.offerhub.campaign.repository;

import com.offerhub.campaign.entity.CaseStatus;
import com.offerhub.campaign.entity.OptimizationCase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OptimizationCaseRepository extends JpaRepository<OptimizationCase, UUID> {

    /**
     * Both filters are optional; a null one means "any". join fetch pulls the campaign in
     * the same query - without it every row would fire its own select for title/priority.
     * Priority cannot be ordered by the column itself: it is stored as text, so the
     * database would sort it alphabetically (DUSUK, KRITIK, ORTA, YUKSEK). The case
     * expression gives the real urgency order, oldest first inside each level.
     */
    @Query(value = """
            select oc from OptimizationCase oc
            join fetch oc.campaign c
            where (:status is null or oc.status = :status)
              and (:assignedExpertId is null or oc.assignedExpertId = :assignedExpertId)
            order by case c.priority
                         when com.offerhub.campaign.entity.Priority.KRITIK then 0
                         when com.offerhub.campaign.entity.Priority.YUKSEK then 1
                         when com.offerhub.campaign.entity.Priority.ORTA then 2
                         else 3
                     end,
                     oc.createdAt
            """,
            countQuery = """
            select count(oc) from OptimizationCase oc
            where (:status is null or oc.status = :status)
              and (:assignedExpertId is null or oc.assignedExpertId = :assignedExpertId)
            """)
    Page<OptimizationCase> search(@Param("status") CaseStatus status,
                                  @Param("assignedExpertId") UUID assignedExpertId,
                                  Pageable pageable);

    /**
     * Same filters as search, ordered by how soon each case runs out instead of how
     * important it is. nulls last keeps cases with no deadline - the ones opened before
     * SLA tracking existed - at the bottom rather than pretending they are the most
     * urgent thing on the screen.
     */
    @Query(value = """
            select oc from OptimizationCase oc
            join fetch oc.campaign c
            where (:status is null or oc.status = :status)
              and (:assignedExpertId is null or oc.assignedExpertId = :assignedExpertId)
            order by oc.slaDeadline asc nulls last, oc.createdAt
            """,
            countQuery = """
            select count(oc) from OptimizationCase oc
            where (:status is null or oc.status = :status)
              and (:assignedExpertId is null or oc.assignedExpertId = :assignedExpertId)
            """)
    Page<OptimizationCase> searchBySla(@Param("status") CaseStatus status,
                                       @Param("assignedExpertId") UUID assignedExpertId,
                                       Pageable pageable);

    /** Published cases whose campaign validity has run out - the system archives these. */
    @Query("""
            select oc from OptimizationCase oc
            join fetch oc.campaign c
            where oc.status = com.offerhub.campaign.entity.CaseStatus.YAYINDA
              and c.validUntil < :now
            """)
    List<OptimizationCase> findExpiredPublished(@Param("now") Instant now);

    /**
     * Cases whose deadline passed while the clock was still running. completedAt is the
     * clock stop, so a null one means still running; a non null slaBreachedAt means this
     * breach was already announced. Both conditions together make the scan idempotent by
     * construction rather than by the scheduler remembering anything.
     */
    @Query("""
            select oc from OptimizationCase oc
            join fetch oc.campaign
            where oc.completedAt is null
              and oc.slaBreachedAt is null
              and oc.slaDeadline is not null
              and oc.slaDeadline < :now
            """)
    List<OptimizationCase> findBreachedBefore(@Param("now") Instant now);

    /** At most one case per campaign - the unique constraint on campaign_id says so. */
    Optional<OptimizationCase> findByCampaignId(UUID campaignId);

    /** Dashboard: cases the SLA can be judged on at all - the rest have no deadline. */
    long countBySlaDeadlineIsNotNull();

    long countBySlaBreachedAtIsNotNull();

    /** Breached and still open: the red card on the supervisor's screen. */
    long countBySlaBreachedAtIsNotNullAndCompletedAtIsNull();

    /** Completed work with an owner - the basis of every expert performance number. */
    @Query("select oc from OptimizationCase oc where oc.assignedExpertId is not null and oc.completedAt is not null")
    List<OptimizationCase> findCompletedWithExpert();

    /** Still open and owned: what each expert is carrying right now. */
    @Query("select oc from OptimizationCase oc where oc.assignedExpertId is not null and oc.completedAt is null")
    List<OptimizationCase> findOpenWithExpert();

    /** Waiting for someone to pick them up. */
    long countByStatus(CaseStatus status);

    /** Detail reads need the campaign too, so fetch it instead of lazy loading it later. */
    @Query("select oc from OptimizationCase oc join fetch oc.campaign where oc.id = :id")
    Optional<OptimizationCase> findByIdWithCampaign(@Param("id") UUID id);
}
