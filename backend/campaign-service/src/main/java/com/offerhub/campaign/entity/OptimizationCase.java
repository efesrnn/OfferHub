package com.offerhub.campaign.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A campaign that needs expert work. Born when AI predicts low conversion - or, while AI
 * Service is not wired in yet, on the fallback path the contract defines for it.
 */
@Entity
@Table(name = "optimization_cases",
        uniqueConstraints = @UniqueConstraint(name = "uk_optimization_cases_campaign", columnNames = "campaign_id"),
        indexes = {
                @Index(name = "idx_optimization_cases_status", columnList = "status"),
                @Index(name = "idx_optimization_cases_expert", columnList = "assigned_expert_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OptimizationCase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * A real foreign key, unlike assignedExpertId: campaigns live in this service's own
     * database. Unique - one case per campaign, enforced by the database, not by code.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CaseStatus status;

    /** Staff user from Identity - soft reference, no FK across service databases. */
    @Column
    private UUID assignedExpertId;

    /** What the expert changed. Required to reach TAMAMLANDI. */
    @Column(length = 1000)
    private String optimizationNote;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    /** Stamped once, on the move to TAMAMLANDI. Also stops the SLA clock. */
    private Instant completedAt;

    /**
     * When this case must be finished, from SlaPolicy. Stored because it is a fact about
     * the case - the countdown derived from it is not, and is computed on every read.
     */
    @Column
    private Instant slaDeadline;

    /**
     * How much the optimization moved the conversion estimate, measured on completion.
     * Positive means the expert's correction helped. Null when AI could not be reached at
     * that moment, which is not the same as zero - zero is a measured no-op.
     */
    @Column(precision = 5, scale = 4)
    private BigDecimal conversionLift;

    /**
     * Stamped the first time the scheduler sees the deadline passed. Its only job is to
     * make the breach a single event - without it every scan would publish sla.breached
     * again and Gamification would keep taking points off the same expert.
     */
    @Column
    private Instant slaBreachedAt;
}
