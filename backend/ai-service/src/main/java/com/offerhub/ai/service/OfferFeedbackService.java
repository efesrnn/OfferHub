package com.offerhub.ai.service;

import com.offerhub.ai.entity.SubscriberProfile;
import com.offerhub.ai.repository.SubscriberProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Feeds a subscriber's answer back into their profile.
 *
 * pastAcceptedOffers and pastDeclinedOffers are model features, so this is not
 * bookkeeping - a declined offer genuinely lowers what the model predicts for that
 * subscriber next time, which is the behaviour the case document asks for in 5.5
 * ("abone ilgilenmiyorum derse benzer kampanyaların öneri skoru düşer").
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OfferFeedbackService {

    private static final String ACCEPTED = "ACCEPTED";

    private final SubscriberProfileRepository subscriberProfileRepository;

    @Transactional
    public void record(String subscriberRef, String response) {
        if (subscriberRef == null || subscriberRef.isBlank()) {
            // A subscriber this service has no profile for; nothing to learn from.
            return;
        }

        SubscriberProfile profile = subscriberProfileRepository.findById(subscriberRef).orElse(null);
        if (profile == null) {
            log.info("No profile for {}, skipping offer feedback", subscriberRef);
            return;
        }

        if (ACCEPTED.equals(response)) {
            profile.setPastAcceptedOffers(profile.getPastAcceptedOffers() + 1);
        } else {
            profile.setPastDeclinedOffers(profile.getPastDeclinedOffers() + 1);
        }
        subscriberProfileRepository.save(profile);

        log.info("Profile {} updated from a {} response (accepted={}, declined={})", subscriberRef,
                response, profile.getPastAcceptedOffers(), profile.getPastDeclinedOffers());
    }
}
