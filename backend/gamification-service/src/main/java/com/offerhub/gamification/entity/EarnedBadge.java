package com.offerhub.gamification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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

/** A badge is earned once. The unique constraint says so, rather than a check in code. */
@Entity
@Table(name = "earned_badges",
        uniqueConstraints = @UniqueConstraint(name = "uk_earned_badges_expert_badge",
                columnNames = {"expert_id", "badge"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EarnedBadge {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID expertId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Badge badge;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant earnedAt;
}
