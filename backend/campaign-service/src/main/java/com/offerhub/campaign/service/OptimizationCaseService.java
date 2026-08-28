package com.offerhub.campaign.service;

import com.offerhub.campaign.dto.AssignRequest;
import com.offerhub.campaign.dto.CaseResponse;
import com.offerhub.campaign.dto.PagedResult;
import com.offerhub.campaign.dto.StatusChangeRequest;
import com.offerhub.campaign.entity.Campaign;
import com.offerhub.campaign.entity.CampaignStatus;
import com.offerhub.campaign.entity.CaseStatus;
import com.offerhub.campaign.entity.OptimizationCase;
import com.offerhub.campaign.exception.ApiException;
import com.offerhub.campaign.exception.ErrorCode;
import com.offerhub.campaign.repository.OptimizationCaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

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

    /** Ordered by priority, most urgent first - the repository query owns that rule. */
    @Transactional(readOnly = true)
    public PagedResult<CaseResponse> list(CaseStatus status, UUID assignedExpertId, Pageable pageable) {
        return PagedResult.from(caseRepository.search(status, assignedExpertId, pageable)
                .map(CaseResponse::from));
    }

    @Transactional(readOnly = true)
    public CaseResponse getById(UUID caseId) {
        return CaseResponse.from(load(caseId));
    }

    /**
     * The only way a case status changes. No save() call below: inside a transaction
     * Hibernate tracks the loaded entities and writes the changed fields on commit.
     * TODO: check the caller's role against the transition table once JWT roles land.
     */
    @Transactional
    public CaseResponse changeStatus(UUID caseId, StatusChangeRequest request) {
        OptimizationCase optimizationCase = load(caseId);
        CaseStatus current = optimizationCase.getStatus();
        CaseStatus target = request.targetStatus();

        CaseStateMachine.assertAllowed(current, target);

        if (target == CaseStatus.TAMAMLANDI) {
            requireOptimizationNote(request.optimizationNote());
            optimizationCase.setCompletedAt(Instant.now());
            // TODO: publish campaign.optimized once RabbitMQ is in place.
        }
        if (StringUtils.hasText(request.optimizationNote())) {
            optimizationCase.setOptimizationNote(request.optimizationNote().trim());
        }

        optimizationCase.setStatus(target);
        applyToCampaign(optimizationCase.getCampaign(), target);

        log.info("Case {} moved {} -> {}", caseId, current, target);
        return CaseResponse.from(optimizationCase);
    }

    /**
     * Supervisor override of the AI assignment. Not a state transition of its own, but it
     * starts a case that nobody had picked up yet.
     * TODO: supervisor only, once JWT roles land.
     */
    @Transactional
    public CaseResponse assign(UUID caseId, AssignRequest request) {
        OptimizationCase optimizationCase = load(caseId);
        CaseStatus current = optimizationCase.getStatus();

        if (!CaseStateMachine.isOpen(current)) {
            throw new ApiException(ErrorCode.INVALID_STATE_TRANSITION,
                    "A case in %s cannot be assigned".formatted(current));
        }

        optimizationCase.setAssignedExpertId(request.expertId());
        if (current == CaseStatus.YENI) {
            optimizationCase.setStatus(CaseStatus.ATANDI);
        }

        log.info("Case {} assigned to expert {}", caseId, request.expertId());
        return CaseResponse.from(optimizationCase);
    }

    private OptimizationCase load(UUID caseId) {
        return caseRepository.findByIdWithCampaign(caseId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Case not found"));
    }

    /** The note says what the expert changed - a completed case without one is unauditable. */
    private static void requireOptimizationNote(String note) {
        if (!StringUtils.hasText(note)) {
            throw new ApiException(ErrorCode.OPTIMIZATION_NOTE_REQUIRED,
                    "An optimization note is required to complete a case");
        }
    }

    /** The case drives its campaign: approval publishes it, archiving retires it. */
    private static void applyToCampaign(Campaign campaign, CaseStatus target) {
        switch (target) {
            case YAYINDA -> campaign.setStatus(CampaignStatus.YAYINDA);
            case ARSIVLENDI -> campaign.setStatus(CampaignStatus.ARSIVLENDI);
            default -> {
            }
        }
    }
}
