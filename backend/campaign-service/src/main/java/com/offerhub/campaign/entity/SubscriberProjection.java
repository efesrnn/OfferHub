package com.offerhub.campaign.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Local read model of subscribers owned by Identity Service.
 * Campaign cannot query another service's database, and a synchronous call would
 * make campaign creation fail whenever Identity is down - so we keep a copy fed by
 * events. Trade-off: eventually consistent. No personal data is copied, only what
 * targeting needs.
 */
@Entity
@Table(name = "subscriber_projection")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriberProjection {

    /** Comes from Identity service. */
    @Id
    private UUID subscriberId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Segment segment;

    @Column(nullable = false)
    private Instant syncedAt;
}