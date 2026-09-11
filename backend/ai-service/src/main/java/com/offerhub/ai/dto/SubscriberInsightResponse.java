package com.offerhub.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Abonenin AI tarafinda nasil goruldugunun okunabilir ozeti - segment (predictSegment ile
 * ayni model) ve o segmenti tetikleyen ham sinyallerden turetilmis kisa bir aciklama.
 * Iki aboneye ayni segment (ornegin PASIF) verilse bile, buradaki "reason" ve ham alanlar
 * onlari birbirinden ayirt etmeye yarar - amac tek bir etiketin arkasindaki gercek veriyi
 * gorunur kilmak.
 */
@Getter
@AllArgsConstructor
public class SubscriberInsightResponse {
    private String subscriberId;
    private String segment;
    private String reason;
    private int tenureMonths;
    private double monthlyDataUsageGb;
    private double monthlySpendTry;
    private int complaintCount6m;
    private double usageTrend;
    private int pastAcceptedOffers;
    private int pastDeclinedOffers;
    private String currentTariff;
}
