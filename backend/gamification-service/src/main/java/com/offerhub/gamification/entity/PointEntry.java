package com.offerhub.gamification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * One scored line, kept so a total can always be explained and audited.
 * The unique constraint is what makes event handling idempotent: RabbitMQ guarantees
 * at-least-once delivery, so the same campaign.optimized can arrive twice. The second
 * insert of the same (sourceId, reason) fails instead of paying the expert twice.
 */
@Entity
@Table(name = "point_entries",
        uniqueConstraints = @UniqueConstraint(name = "uk_point_entries_source_reason",
                columnNames = {"source_id", "reason"}),
        indexes = {
                @Index(name = "idx_point_entries_expert", columnList = "expert_id"),
                @Index(name = "idx_point_entries_earned_at", columnList = "earned_at")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID expertId;

    /** The case or offer the points came from - the deduplication key together with reason. */
    @Column(nullable = false)
    private UUID sourceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private PointReason reason;

    /** Copied from the reason on write: changing the table later must not rewrite history. */
    @Column(nullable = false)
    private Integer points;

    /** Kept for the segment based badges; a plain string, this service owns no segment enum. */
    @Column(length = 20)
    private String segment;

    @CreationTimestamp
    @Column(name = "earned_at", updatable = false)
    private Instant earnedAt;
}
