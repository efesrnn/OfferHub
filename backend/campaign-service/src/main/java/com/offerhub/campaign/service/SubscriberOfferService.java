package com.offerhub.campaign.service;

import com.offerhub.campaign.dto.OfferResponse;
import com.offerhub.campaign.entity.Campaign;
import com.offerhub.campaign.entity.CampaignStatus;
import com.offerhub.campaign.entity.OfferStatus;
import com.offerhub.campaign.entity.SubscriberOffer;
import com.offerhub.campaign.event.OfferRatedPayload;
import com.offerhub.campaign.event.OfferRespondedPayload;
import com.offerhub.campaign.event.OutboundEvent;
import com.offerhub.campaign.exception.ApiException;
import com.offerhub.campaign.exception.ErrorCode;
import com.offerhub.campaign.repository.CampaignRepository;
import com.offerhub.campaign.repository.SubscriberOfferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class SubscriberOfferService {

    private final SubscriberOfferRepository offerRepository;
    private final CampaignRepository campaignRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Segment eslestirmesi henuz yok (SubscriberProjection senkronu kurulmadi) - simdilik
     * yayinda ve suresi dolmamis her kampanya her aboneye teklif olarak gosterilir.
     * Ilk goruntulemede eksik olan teklifler PENDING olarak olusturulur (lazy materialize).
     */
    @Transactional
    public List<OfferResponse> listMyOffers(UUID subscriberId) {
        List<SubscriberOffer> existing = offerRepository.findBySubscriberIdOrderByCreatedAtDesc(subscriberId);
        Set<UUID> trackedCampaignIds = existing.stream()
                .map(o -> o.getCampaign().getId())
                .collect(Collectors.toSet());

        List<Campaign> eligible = campaignRepository.findByStatusAndValidUntilAfter(
                CampaignStatus.YAYINDA, Instant.now());

        List<SubscriberOffer> newlyCreated = eligible.stream()
                .filter(c -> !trackedCampaignIds.contains(c.getId()))
                .map(c -> SubscriberOffer.builder()
                        .campaign(c)
                        .subscriberId(subscriberId)
                        .status(OfferStatus.PENDING)
                        .build())
                .toList();

        if (!newlyCreated.isEmpty()) {
            offerRepository.saveAll(newlyCreated);
        }

        return Stream.concat(existing.stream(), newlyCreated.stream())
                .map(OfferResponse::from)
                .sorted(Comparator.comparingDouble(OfferResponse::score).reversed())
                .toList();
    }

    @Transactional(readOnly = true)
    public OfferResponse getOne(UUID subscriberId, UUID offerId) {
        return OfferResponse.from(load(subscriberId, offerId));
    }

    @Transactional
    public OfferResponse respond(UUID subscriberId, UUID offerId, boolean accept) {
        SubscriberOffer offer = load(subscriberId, offerId);
        if (offer.getStatus() != OfferStatus.PENDING) {
            throw new ApiException(ErrorCode.OFFER_ALREADY_RESPONDED, "Bu teklife zaten yanit verdiniz");
        }

        offer.setStatus(accept ? OfferStatus.ACCEPTED : OfferStatus.DECLINED);
        offer.setRespondedAt(Instant.now());

        eventPublisher.publishEvent(new OutboundEvent(
                OutboundEvent.OFFER_RESPONDED, OfferRespondedPayload.from(offer)));

        return OfferResponse.from(offer);
    }

    @Transactional
    public OfferResponse rate(UUID subscriberId, UUID offerId, Integer rating) {
        if (rating == null || rating < 1 || rating > 5) {
            throw new ApiException(ErrorCode.INVALID_RATING, "Puan 1 ile 5 arasinda olmalidir");
        }

        SubscriberOffer offer = load(subscriberId, offerId);
        if (offer.getStatus() != OfferStatus.ACCEPTED) {
            throw new ApiException(ErrorCode.OFFER_NOT_ACCEPTED, "Sadece kabul edilen tekliflere puan verilebilir");
        }
        if (offer.getRating() != null) {
            throw new ApiException(ErrorCode.OFFER_ALREADY_RATED, "Bu teklife zaten puan verdiniz");
        }

        offer.setRating(rating);
        offer.setRatedAt(Instant.now());

        eventPublisher.publishEvent(new OutboundEvent(
                OutboundEvent.OFFER_RATED, OfferRatedPayload.from(offer)));

        return OfferResponse.from(offer);
    }

    /** IDOR guard: offerId + subscriberId birlikte aranir, baskasinin teklifi asla donmez. */
    SubscriberOffer load(UUID subscriberId, UUID offerId) {
        return offerRepository.findByIdAndSubscriberId(offerId, subscriberId)
                .orElseThrow(() -> new ApiException(ErrorCode.OFFER_NOT_FOUND, "Teklif bulunamadi"));
    }
}
