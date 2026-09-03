package com.offerhub.campaign.service;

import com.offerhub.campaign.dto.CampaignResponse;
import com.offerhub.campaign.dto.CreateCampaignRequest;
import com.offerhub.campaign.dto.PagedResult;
import com.offerhub.campaign.dto.ClassificationRequest;
import com.offerhub.campaign.entity.Campaign;
import com.offerhub.campaign.entity.CampaignStatus;
import com.offerhub.campaign.entity.Priority;
import com.offerhub.campaign.entity.Segment;
import com.offerhub.campaign.event.CampaignCreatedPayload;
import com.offerhub.campaign.event.OutboundEvent;
import com.offerhub.campaign.event.SegmentChangedPayload;
import com.offerhub.campaign.exception.ApiException;
import com.offerhub.campaign.exception.ErrorCode;
import com.offerhub.campaign.repository.CampaignRepository;
import com.offerhub.campaign.security.CallerIdentity;
import com.offerhub.campaign.security.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final CampaignNumberGenerator numberGenerator;
    private final OptimizationCaseService caseService;
    private final CampaignAiAdvisor aiAdvisor;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Case document 5.1: targeting a segment sends the campaign to AI for a conversion
     * estimate, a classification and a priority. If AI cannot be reached the campaign is
     * still created - it simply arrives unscored and lands in the manual queue.
     *
     * The AI call happens before the transaction does any writing, so a slow answer costs
     * time but never holds a row lock.
     */
    @Transactional
    public CampaignResponse create(CreateCampaignRequest request, CallerIdentity caller) {
        CampaignScoring scoring = aiAdvisor.scoreFor(request.targetSegment(), request.type());

        Campaign campaign = Campaign.builder()
                .campaignNo(numberGenerator.next())
                .title(request.title())
                .type(request.type())
                .targetSegment(request.targetSegment())
                // aiSegment and segment start equal; only segment moves on an override,
                // which is what makes the pair a record of AI being corrected.
                .aiSegment(scoring.segment())
                .segment(scoring.segment())
                .discountRate(request.discountRate())
                .validUntil(request.validUntil())
                .status(CampaignStatus.YENI)
                .priority(scoring.priority())
                .conversionProbability(scoring.conversionProbability())
                .recommendationScore(scoring.recommendationScore())
                .createdBy(caller.userId())
                .build();

        // saveAndFlush, not save: @CreationTimestamp is filled during the insert, and
        // save() defers that to commit - the response would carry createdAt = null.
        Campaign saved = campaignRepository.saveAndFlush(campaign);

        // Same transaction: a campaign that should be optimized never exists without its case.
        caseService.openIfLowConversion(saved);

        eventPublisher.publishEvent(new OutboundEvent(
                OutboundEvent.CAMPAIGN_CREATED, CampaignCreatedPayload.from(saved)));

        return CampaignResponse.from(saved);
    }

    /**
     * An expert sees only campaigns they are part of. The role matrix says "kendi
     * kayitlari (atanan)", and knowing the list endpoint exists must not be enough to
     * read the whole company's campaign book.
     */
    @Transactional(readOnly = true)
    public PagedResult<CampaignResponse> list(CampaignStatus status, Segment segment,
                                              CallerIdentity caller, Pageable pageable) {
        UUID expertId = caller.isExpert() ? caller.userId() : null;
        return PagedResult.from(campaignRepository.search(status, segment, expertId, pageable)
                .map(CampaignResponse::from));
    }

    @Transactional(readOnly = true)
    public CampaignResponse getByCampaignNo(String campaignNo) {
        return CampaignResponse.from(load(campaignNo));
    }

    /**
     * Correcting AI's classification. aiSegment is deliberately left alone: it is the
     * baseline AI's accuracy is measured against, so overwriting it would erase the very
     * mistake this endpoint exists to record.
     */
    @Transactional
    public CampaignResponse reclassify(String campaignNo, ClassificationRequest request,
                                       CallerIdentity caller) {
        if (request.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR,
                    "at least one of segment, type or priority must be given");
        }
        // Case document 5.3 gives priority to the supervisor alone; segment and type are
        // one permission shared with the expert, which the controller already checked.
        if (request.priority() != null) {
            caller.requireAnyOf(Role.SUPERVISOR);
        }

        Campaign campaign = load(campaignNo);
        Segment original = campaign.getSegment();

        applyType(campaign, request, caller);
        applyPriority(campaign, request, caller);
        applySegment(campaign, request, caller, original);

        return CampaignResponse.from(campaign);
    }

    /**
     * Setting a value it already has corrects nothing, so it announces nothing. PATCH is
     * expected to be repeatable; a retry must not look like a second mistake to AI.
     */
    private void applySegment(Campaign campaign, ClassificationRequest request,
                              CallerIdentity caller, Segment original) {
        if (request.segment() == null || request.segment() == original) {
            return;
        }

        campaign.setSegment(request.segment());
        applyChurnPriorityFloor(campaign);

        eventPublisher.publishEvent(new OutboundEvent(OutboundEvent.SEGMENT_CHANGED,
                new SegmentChangedPayload(campaign.getCampaignNo(), caller.userId(), caller.role(),
                        original, request.segment())));

        log.info("Campaign {} segment corrected {} -> {} by {} ({}), reason: {}",
                campaign.getCampaignNo(), original, request.segment(),
                caller.userId(), caller.role(), request.reason());
    }

    private void applyType(Campaign campaign, ClassificationRequest request, CallerIdentity caller) {
        if (request.type() == null || request.type() == campaign.getType()) {
            return;
        }
        log.info("Campaign {} type corrected {} -> {} by {}, reason: {}", campaign.getCampaignNo(),
                campaign.getType(), request.type(), caller.userId(), request.reason());
        campaign.setType(request.type());
    }

    /**
     * A manual priority overrides what AI derived, and the SLA window follows it - the
     * deadline is a function of priority, so leaving it behind would let a case be judged
     * against an urgency it no longer has.
     */
    private void applyPriority(Campaign campaign, ClassificationRequest request, CallerIdentity caller) {
        if (request.priority() == null || request.priority() == campaign.getPriority()) {
            return;
        }
        log.info("Campaign {} priority set {} -> {} by supervisor {}, reason: {}",
                campaign.getCampaignNo(), campaign.getPriority(), request.priority(),
                caller.userId(), request.reason());

        campaign.setPriority(request.priority());
        caseService.recalculateSlaDeadline(campaign);
    }

    private void applyChurnPriorityFloor(Campaign campaign) {
        if (campaign.getSegment() != Segment.RISKLI_KAYIP
                || campaign.getPriority().compareTo(Priority.YUKSEK) >= 0) {
            return;
        }

        log.info("Campaign {} priority raised {} -> YUKSEK, churn risk segment",
                campaign.getCampaignNo(), campaign.getPriority());
        campaign.setPriority(Priority.YUKSEK);
        caseService.recalculateSlaDeadline(campaign);
    }

    private Campaign load(String campaignNo) {
        return campaignRepository.findByCampaignNo(campaignNo)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Campaign not found"));
    }
}