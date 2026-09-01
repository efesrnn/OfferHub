package com.offerhub.campaign.service;

import com.offerhub.campaign.dto.CampaignResponse;
import com.offerhub.campaign.dto.CreateCampaignRequest;
import com.offerhub.campaign.dto.PagedResult;
import com.offerhub.campaign.entity.Campaign;
import com.offerhub.campaign.entity.CampaignStatus;
import com.offerhub.campaign.entity.Priority;
import com.offerhub.campaign.entity.Segment;
import com.offerhub.campaign.event.CampaignCreatedPayload;
import com.offerhub.campaign.event.OutboundEvent;
import com.offerhub.campaign.exception.ApiException;
import com.offerhub.campaign.exception.ErrorCode;
import com.offerhub.campaign.repository.CampaignRepository;
import com.offerhub.campaign.security.CallerIdentity;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final CampaignNumberGenerator numberGenerator;
    private final OptimizationCaseService caseService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * AI Service is not wired in yet, so every campaign takes the fallback path the
     * contract already defines: segment BELIRSIZ, priority ORTA, still created.
     */
    @Transactional
    public CampaignResponse create(CreateCampaignRequest request, CallerIdentity caller) {
        Campaign campaign = Campaign.builder()
                .campaignNo(numberGenerator.next())
                .title(request.title())
                .type(request.type())
                .targetSegment(request.targetSegment())
                .aiSegment(Segment.BELIRSIZ)
                .segment(Segment.BELIRSIZ)
                .discountRate(request.discountRate())
                .validUntil(request.validUntil())
                .status(CampaignStatus.YENI)
                .priority(Priority.ORTA)
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

    @Transactional(readOnly = true)
    public PagedResult<CampaignResponse> list(CampaignStatus status, Segment segment, Pageable pageable) {
        return PagedResult.from(campaignRepository.search(status, segment, pageable)
                .map(CampaignResponse::from));
    }

    @Transactional(readOnly = true)
    public CampaignResponse getByCampaignNo(String campaignNo) {
        return campaignRepository.findByCampaignNo(campaignNo)
                .map(CampaignResponse::from)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Campaign not found"));
    }
}