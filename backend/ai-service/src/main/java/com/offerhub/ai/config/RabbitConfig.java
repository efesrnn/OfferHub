package com.offerhub.ai.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI listens instead of being called.
 *
 * The alternative was for Campaign to POST to /ai/misclassification whenever an expert
 * corrects a segment. That would make correcting a segment fail whenever AI is down, and
 * it is the wrong dependency direction: recording that a prediction was wrong is AI's
 * concern, not something the campaign flow should wait for. EVENTS.md already specified
 * these two routing keys for this service.
 */
@Configuration
public class RabbitConfig {

    /** Declared by every participant so start order does not matter. */
    public static final String EVENTS_EXCHANGE = "offerhub.events";

    /** Durable: a correction made while AI is restarting must not be lost. */
    public static final String EVENTS_QUEUE = "ai.events";

    public static final String SEGMENT_CHANGED = "segment.changed";
    public static final String OFFER_RESPONDED = "offer.responded";

    @Bean
    public TopicExchange offerhubEventsExchange() {
        return new TopicExchange(EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public Queue aiEventsQueue() {
        return new Queue(EVENTS_QUEUE, true);
    }

    @Bean
    public Binding segmentChangedBinding(Queue aiEventsQueue, TopicExchange offerhubEventsExchange) {
        return BindingBuilder.bind(aiEventsQueue).to(offerhubEventsExchange).with(SEGMENT_CHANGED);
    }

    @Bean
    public Binding offerRespondedBinding(Queue aiEventsQueue, TopicExchange offerhubEventsExchange) {
        return BindingBuilder.bind(aiEventsQueue).to(offerhubEventsExchange).with(OFFER_RESPONDED);
    }
}
