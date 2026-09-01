package com.offerhub.gamification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Running totals per expert. Could be recomputed from point_entries every time, but the
 * profile screen and the leaderboard read this constantly - so it is kept up to date.
 */
@Entity
@Table(name = "expert_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpertProfile {

    /** Staff user from Identity - soft reference, no FK across service databases. */
    @Id
    private UUID expertId;

    @Column(nullable = false)
    private Integer totalPoints;

    /** Completed optimizations, not point entries - one case can produce several entries. */
    @Column(nullable = false)
    private Integer casesResolved;

    @UpdateTimestamp
    private Instant updatedAt;

    public static ExpertProfile empty(UUID expertId) {
        return ExpertProfile.builder()
                .expertId(expertId)
                .totalPoints(0)
                .casesResolved(0)
                .build();
    }
}
