package com.offerhub.ai.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "expert_snapshots")
@Getter
@Setter
@NoArgsConstructor
public class ExpertSnapshot {

    @Id
    @Column(name = "expert_id")
    private String expertId;

    @ElementCollection
    @CollectionTable(name = "expert_snapshot_specialties", joinColumns = @JoinColumn(name = "expert_id"))
    @Column(name = "specialty")
    private List<String> specialties;

    @Column(name = "active_case_count")
    private int activeCaseCount;

    @Column(name = "performance_score")
    private double performanceScore;

    public static final int MAX_CAPACITY = 10;
}