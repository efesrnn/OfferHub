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

import java.time.Instant;
import java.util.UUID;

/** Abone-kampanya eslemesi: bir aboneye gosterilen tek bir teklifin durumu. */
@Entity
@Table(name = "subscriber_offers",
        uniqueConstraints = @UniqueConstraint(name = "uk_subscriber_offers_subscriber_campaign",
                columnNames = {"subscriber_id", "campaign_id"}),
        indexes = {
                @Index(name = "idx_subscriber_offers_subscriber", columnList = "subscriber_id"),
                @Index(name = "idx_subscriber_offers_status", columnList = "status")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriberOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    /** Identity'nin subscriber id'si - soft reference, servisler arasi FK yok. */
    @Column(name = "subscriber_id", nullable = false)
    private UUID subscriberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OfferStatus status;

    private Instant respondedAt;

    private Integer rating;

    private Instant ratedAt;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;
}
