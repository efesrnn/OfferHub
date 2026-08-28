package com.offerhub.campaign.repository;

import com.offerhub.campaign.entity.CaseStatus;
import com.offerhub.campaign.entity.OptimizationCase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /** Detail reads need the campaign too, so fetch it instead of lazy loading it later. */
    @Query("select oc from OptimizationCase oc join fetch oc.campaign where oc.id = :id")
    Optional<OptimizationCase> findByIdWithCampaign(@Param("id") UUID id);
}
