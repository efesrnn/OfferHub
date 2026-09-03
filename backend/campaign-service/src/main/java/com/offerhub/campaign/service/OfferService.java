package com.offerhub.campaign.service;

import com.offerhub.campaign.client.AiClient;
import com.offerhub.campaign.client.AiRecommendation;
import com.offerhub.campaign.dto.OfferRateRequest;
import com.offerhub.campaign.dto.OfferRespondRequest;
import com.offerhub.campaign.dto.OfferResponse;
import com.offerhub.campaign.dto.PagedResult;
import com.offerhub.campaign.entity.Campaign;
import com.offerhub.campaign.entity.Offer;
import com.offerhub.campaign.entity.OfferStatus;
import com.offerhub.campaign.entity.OptimizationCase;
import com.offerhub.campaign.entity.Segment;
import com.offerhub.campaign.entity.SubscriberProjection;
import com.offerhub.campaign.event.OfferRatedPayload;
import com.offerhub.campaign.event.OfferRespondedPayload;
import com.offerhub.campaign.event.OutboundEvent;
import com.offerhub.campaign.exception.ApiException;
import com.offerhub.campaign.exception.ErrorCode;
import com.offerhub.campaign.repository.CampaignRepository;
import com.offerhub.campaign.repository.OfferRepository;
import com.offerhub.campaign.repository.OptimizationCaseRepository;
import com.offerhub.campaign.repository.SubscriberProjectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OfferService {

    private static final int LOW_RATING_MAX_STARS = 2;

    /** Case document 6.1: below this the campaign is not worth the subscriber's attention. */
    private static final BigDecimal MIN_VISIBLE_SCORE = new BigDecimal("0.60");

    private final OfferRepository offerRepository;
    private final CampaignRepository campaignRepository;
    private final OptimizationCaseRepository caseRepository;
    private final SubscriberProjectionRepository projectionRepository;
    private final AiClient aiClient;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * The subscriber's offer list. Offers are materialised here rather than pushed out
     * when a campaign is created: a subscriber who registers tomorrow still sees today's
     * campaigns, and no row is written for a campaign nobody ever opens.
     */
    @Transactional
    public PagedResult<OfferResponse> listFor(UUID subscriberId, Pageable pageable) {
        materialise(subscriberId);
        return PagedResult.from(offerRepository.findForSubscriber(subscriberId, pageable)
                .map(OfferResponse::from));
    }

    @Transactional
    public OfferResponse respond(UUID offerId, OfferRespondRequest request, UUID subscriberId) {
        if (request.response() == OfferStatus.PENDING) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR,
                    "response must be ACCEPTED or DECLINED");
        }

        Offer offer = load(offerId, subscriberId);
        if (offer.getStatus() != OfferStatus.PENDING) {
            throw new ApiException(ErrorCode.OFFER_ALREADY_RESPONDED,
                    "This offer was already answered");
        }

        offer.setStatus(request.response());
        offer.setRespondedAt(Instant.now());

        eventPublisher.publishEvent(new OutboundEvent(
                OutboundEvent.OFFER_RESPONDED, OfferRespondedPayload.from(offer)));

        log.info("Offer {} answered {}", offerId, request.response());
        return OfferResponse.from(offer);
    }

    /** Rating is once and final, as the case document puts it: "tek seferlik". */
    @Transactional
    public OfferResponse rate(UUID offerId, OfferRateRequest request, UUID subscriberId) {
        Offer offer = load(offerId, subscriberId);
        if (offer.getStars() != null) {
            throw new ApiException(ErrorCode.OFFER_ALREADY_RATED, "This offer was already rated");
        }

        offer.setStars(request.stars());
        offer.setRatedAt(Instant.now());

        // The expert who worked this campaign is who a low rating reflects on. Gamification
        // cannot resolve that - campaigns and cases live only in this service.
        UUID expertId = caseRepository.findByCampaignId(offer.getCampaign().getId())
                .map(OptimizationCase::getAssignedExpertId)
                .orElse(null);

        eventPublisher.publishEvent(new OutboundEvent(
                OutboundEvent.OFFER_RATED, OfferRatedPayload.from(offer, expertId)));

        if (request.stars() <= LOW_RATING_MAX_STARS) {
            log.info("Offer {} rated {} stars, expert {} loses points",
                    offerId, request.stars(), expertId);
        }
        return OfferResponse.from(offer);
    }

    /**
     * Creates the offers this subscriber does not have yet. The unique constraint on
     * (subscriber, campaign) is the real guard; this check only avoids provoking it.
     */
    private void materialise(UUID subscriberId) {
        SubscriberProjection subscriber = projectionOf(subscriberId);
        Segment segment = knownSegment(subscriber);
        Set<UUID> alreadyOffered = offerRepository.findCampaignIdsFor(subscriberId);

        List<Offer> fresh = campaignRepository.findOfferable(segment, Instant.now()).stream()
                .filter(campaign -> !alreadyOffered.contains(campaign.getId()))
                .map(campaign -> newOffer(subscriber, campaign))
                .filter(OfferService::worthShowing)
                .toList();

        if (!fresh.isEmpty()) {
            offerRepository.saveAll(fresh);
            log.info("Created {} offer(s) for subscriber {}", fresh.size(), subscriberId);
        }
    }

    /**
     * Scores the campaign for this particular subscriber, which is what AI is actually for -
     * the campaign level score computed at creation is an average over a segment, this is
     * the individual one the offer list is ranked by.
     */
    private Offer newOffer(SubscriberProjection subscriber, Campaign campaign) {
        return Offer.builder()
                .subscriberId(subscriber.getSubscriberId())
                .campaign(campaign)
                .score(scoreFor(subscriber, campaign))
                .status(OfferStatus.PENDING)
                .build();
    }

    /**
     * Null when we cannot honestly produce a number: AI is down, or this subscriber is not
     * one AI knows.
     *
     * The second case is deliberate. AI answers for an unknown id by synthesising a profile
     * from the id itself, and a fabricated score would then decide which real campaigns a
     * real subscriber never gets to see. Better no score than an invented one.
     */
    private BigDecimal scoreFor(SubscriberProjection subscriber, Campaign campaign) {
        String reference = subscriber.getExternalRef();
        if (reference == null || reference.isBlank()) {
            return null;
        }
        return aiClient.recommend(reference, campaign.getType())
                .map(AiRecommendation::score)
                .orElse(null);
    }

    /**
     * Case document 6.1: a campaign scoring under 0.60 is not shown. An unscored offer is
     * kept - the threshold filters campaigns we judged poorly, not ones we could not judge.
     */
    private static boolean worthShowing(Offer offer) {
        return offer.getScore() == null || offer.getScore().compareTo(MIN_VISIBLE_SCORE) >= 0;
    }

    /**
     * Identity does not announce new subscribers yet, so the first time one asks for
     * offers we record what we know, which is nothing. BELIRSIZ is the fallback the
     * contract already defines for an unclassified subscriber, and it is turned into null
     * here so that no campaign gets filtered out for them.
     */
    private SubscriberProjection projectionOf(UUID subscriberId) {
        return projectionRepository.findById(subscriberId)
                .orElseGet(() -> projectionRepository.save(SubscriberProjection.builder()
                        .subscriberId(subscriberId)
                        .segment(Segment.BELIRSIZ)
                        .syncedAt(Instant.now())
                        .build()));
    }

    /** BELIRSIZ becomes null so that nothing is filtered out for a subscriber we cannot place. */
    private static Segment knownSegment(SubscriberProjection projection) {
        return projection.getSegment() == Segment.BELIRSIZ ? null : projection.getSegment();
    }

    private Offer load(UUID offerId, UUID subscriberId) {
        Offer offer = offerRepository.findByIdWithCampaign(offerId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Offer not found"));

        if (!offer.getSubscriberId().equals(subscriberId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "This offer belongs to another subscriber");
        }
        return offer;
    }
}
