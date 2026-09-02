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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * One campaign put in front of one subscriber. The unique constraint is the rule that a
 * subscriber is shown a campaign once - enforced by the database, so a repeated read
 * cannot quietly produce a second offer for the same pair.
 */
@Entity
@Table(name = "offers",
        uniqueConstraints = @UniqueConstraint(name = "uk_offers_subscriber_campaign",
                columnNames = {"subscriber_id", "campaign_id"}),
        indexes = @Index(name = "idx_offers_subscriber", columnList = "subscriber_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Offer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Subscriber from Identity - soft reference, no FK across service databases. */
    @Column(name = "subscriber_id", nullable = false)
    private UUID subscriberId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OfferStatus status;

    /** AI recommendation score 0.000-1.000; null until AI Service is wired in. */
    @Column(precision = 4, scale = 3)
    private BigDecimal score;

    /** 1-5, set once. Null means the subscriber has not rated this offer. */
    private Integer stars;

    private Instant ratedAt;

    private Instant respondedAt;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;
}
