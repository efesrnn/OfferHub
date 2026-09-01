package com.offerhub.campaign.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Per-year counter behind campaign numbers (CMP-2026-000123).
 * A table rather than a Postgres sequence so it is created by ddl-auto and resets each year.
 */
@Entity
@Table(name = "campaign_counters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CampaignCounter {

    @Id
    @Column(name = "counter_year")
    private Integer year;

    @Column(nullable = false)
    private Integer lastValue;
}