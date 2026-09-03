package com.offerhub.ai.event;

/**
 * Our own copy of the offer.responded payload.
 *
 * subscriberRef is the readable code this service keys its profiles by. The UUID in the
 * same payload identifies the subscriber for everyone else, but it is not something this
 * service can look a profile up with.
 */
public record OfferRespondedEvent(
        String subscriberRef,
        String campaignNo,
        String response
) {
}
