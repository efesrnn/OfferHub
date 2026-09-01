package com.offerhub.campaign.entity;

/**
 * Optimization case lifecycle. Different from CampaignStatus: the campaign itself only
 * knows YENI/YAYINDA/ARSIVLENDI, the full workflow belongs to the case.
 */
public enum CaseStatus {
    YENI,
    ATANDI,
    OPTIMIZE_EDILIYOR,
    TEST_EDILIYOR,
    TAMAMLANDI,
    YAYINDA,
    ARSIVLENDI
}
