package com.offerhub.ai.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "segment_prediction_counters")
@Getter
@Setter
@NoArgsConstructor
public class SegmentPredictionCounter {

    @Id
    private String segment;

    private long totalCount;

    public SegmentPredictionCounter(String segment, long totalCount) {
        this.segment = segment;
        this.totalCount = totalCount;
    }
}
