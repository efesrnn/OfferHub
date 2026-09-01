package com.offerhub.ai.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "misclassification_logs")
@Getter
@Setter
@NoArgsConstructor
public class MisclassificationLog {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "campaign_no")
    private String campaignNo;

    @Column(name = "original_segment")
    private String originalSegment;

    @Column(name = "corrected_segment")
    private String correctedSegment;

    @Column(name = "created_at")
    private Instant createdAt;

    public MisclassificationLog(String campaignNo, String originalSegment, String correctedSegment) {
        this.campaignNo = campaignNo;
        this.originalSegment = originalSegment;
        this.correctedSegment = correctedSegment;
        this.createdAt = Instant.now();
    }
}