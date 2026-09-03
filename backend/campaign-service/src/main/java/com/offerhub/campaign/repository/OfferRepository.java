package com.offerhub.campaign.repository;

import com.offerhub.campaign.entity.Offer;
import com.offerhub.campaign.entity.OfferStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface OfferRepository extends JpaRepository<Offer, UUID> {

    /**
     * Highest score first, as the contract promises the client. nulls last keeps unscored
     * offers below scored ones instead of at the top, which is where a null would land in
     * a descending sort; newest first breaks the tie while AI is not wired in and every
     * score is null.
     *
     * The score filter is case document 6.1: a campaign scoring under 0.60 is not shown.
     * An unscored offer is kept - the rule hides campaigns we judged poorly, not ones we
     * could not judge.
     */
    @Query(value = """
            select o from Offer o
            join fetch o.campaign
            where o.subscriberId = :subscriberId
              and (o.score is null or o.score >= :minScore)
            order by o.score desc nulls last, o.createdAt desc
            """,
            countQuery = """
            select count(o) from Offer o
            where o.subscriberId = :subscriberId
              and (o.score is null or o.score >= :minScore)
            """)
    Page<Offer> findForSubscriber(@Param("subscriberId") UUID subscriberId,
                                  @Param("minScore") BigDecimal minScore,
                                  Pageable pageable);

    /** Which campaigns this subscriber already has an offer for - the rest are new. */
    @Query("select o.campaign.id from Offer o where o.subscriberId = :subscriberId")
    Set<UUID> findCampaignIdsFor(@Param("subscriberId") UUID subscriberId);

    @Query("select o from Offer o join fetch o.campaign where o.id = :id")
    Optional<Offer> findByIdWithCampaign(@Param("id") UUID id);

    /** Answered offers since a date, for the conversion trend. Small set, grouped in Java. */
    @Query("select o from Offer o where o.respondedAt is not null and o.respondedAt >= :since")
    List<Offer> findAnsweredSince(@Param("since") Instant since);

    /** Dashboard: offers the subscriber actually answered, the denominator of conversion. */
    long countByStatusNot(OfferStatus status);

    long countByStatus(OfferStatus status);
}
