package com.offerhub.campaign.service;

import com.offerhub.campaign.entity.Campaign;
import com.offerhub.campaign.entity.CaseStatus;
import com.offerhub.campaign.entity.OptimizationCase;
import com.offerhub.campaign.repository.OptimizationCaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class OptimizationCaseService {

    /** Below this predicted conversion a campaign is worth an expert's time. */
    private static final BigDecimal LOW_CONVERSION_THRESHOLD = new BigDecimal("0.60");

    private final OptimizationCaseRepository caseRepository;

    /**
     * A null probability means AI Service never answered. The contract puts that campaign
     * in the manual optimization queue, so null counts as low - the fallback path is the
     * only path until AI Service is wired in.
     */
    @Transactional
    public void openIfLowConversion(Campaign campaign) {
        BigDecimal probability = campaign.getConversionProbability();
        if (probability != null && probability.compareTo(LOW_CONVERSION_THRESHOLD) >= 0) {
            return;
        }

        OptimizationCase optimizationCase = caseRepository.save(OptimizationCase.builder()
                .campaign(campaign)
                .status(CaseStatus.YENI)
                .build());

        log.info("Opened case {} for campaign {} (conversionProbability={})",
                optimizationCase.getId(), campaign.getCampaignNo(), probability);
    }
}
