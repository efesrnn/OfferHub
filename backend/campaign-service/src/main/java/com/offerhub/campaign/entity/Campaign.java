package com.offerhub.campaign.entity;

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
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "campaigns",
        uniqueConstraints = @UniqueConstraint(name = "uk_campaigns_campaign_no", columnNames = "campaign_no"),
        indexes = {
                @Index(name = "idx_campaigns_status", columnList = "status"),
                @Index(name = "idx_campaigns_segment", columnList = "segment"),
                @Index(name = "idx_campaigns_valid_until", columnList = "valid_until")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Campaign {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** CMP-2026-000123. */
    @Column(nullable = false, length = 20)
    private String campaignNo;

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CampaignType type;

    /** What the expert aimed at on creation. Never changes. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Segment targetSegment;

    /** What AI classified it as. Never changes - it is the baseline for accuracy tracking. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Segment aiSegment;

    /** Currently effective segment. Starts as aiSegment, changes on if expert override. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Segment segment;

    @Column(nullable = false)
    private Integer discountRate;

    @Column(nullable = false)
    private Instant validUntil;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CampaignStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Priority priority;

    /** AI prediction 0.000-1.000; null until AI Service is wired in. */
    @Column(precision = 4, scale = 3)
    private BigDecimal conversionProbability;

    /** Staff user from Identity - soft reference, no FK across service databases. */
    @Column
    private UUID createdBy;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}