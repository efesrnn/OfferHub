package com.offerhub.ai.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "subscriber_profiles")
@Getter
@Setter
@NoArgsConstructor
public class SubscriberProfile {

    @Id
    @Column(name = "subscriber_id")
    private String subscriberId;

    private int tenureMonths;
    private double monthlyDataUsageGb;
    private double monthlyVoiceMinutes;
    private double monthlySpendTry;
    private String currentTariff;
    private int pastAcceptedOffers;
    private int pastDeclinedOffers;
    private int complaintCount6m;
    private double usageTrend;
}