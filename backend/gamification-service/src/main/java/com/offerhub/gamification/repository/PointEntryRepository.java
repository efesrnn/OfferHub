package com.offerhub.gamification.repository;

import com.offerhub.gamification.entity.PointEntry;
import com.offerhub.gamification.entity.PointReason;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PointEntryRepository extends JpaRepository<PointEntry, UUID> {

    /** Cheap guard before the insert; the unique constraint is the real one. */
    boolean existsBySourceIdAndReason(UUID sourceId, PointReason reason);

    long countByExpertIdAndReason(UUID expertId, PointReason reason);

    long countByExpertIdAndReasonAndSegment(UUID expertId, PointReason reason, String segment);

    /** Backs the Maratoncu badge: how much was done inside a moving 24 hour window. */
    long countByExpertIdAndReasonAndEarnedAtAfter(UUID expertId, PointReason reason, Instant after);

    /** Backs the Uzman badge: the caller takes the largest of these counts. */
    @Query("""
            select count(p) from PointEntry p
            where p.expertId = :expertId and p.reason = :reason and p.segment is not null
            group by p.segment
            """)
    List<Long> countsPerSegment(@Param("expertId") UUID expertId, @Param("reason") PointReason reason);
}
