package com.offerhub.campaign.event;

/**
 * Raised inside a transaction, sent to RabbitMQ only after that transaction commits.
 * Services publish this instead of talking to RabbitTemplate directly.
 */
public record OutboundEvent(String eventType, Object payload) {

    public static final String CAMPAIGN_CREATED = "campaign.created";
    public static final String CAMPAIGN_OPTIMIZED = "campaign.optimized";
    public static final String SEGMENT_CHANGED = "segment.changed";
    public static final String OFFER_RESPONDED = "offer.responded";
    public static final String OFFER_RATED = "offer.rated";
    public static final String SLA_BREACHED = "sla.breached";
}
