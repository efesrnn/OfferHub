package com.offerhub.ai.event;

import com.offerhub.ai.config.RabbitConfig;
import com.offerhub.ai.service.AccuracyService;
import com.offerhub.ai.service.OfferFeedbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;

/**
 * Reads the raw JSON rather than letting a converter map it to a class. The publisher
 * writes its own type name into the message headers and that class does not exist here;
 * parsing the body ourselves keeps the two services independent, which was the point of
 * putting JSON on the wire.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiEventListener {

    private final JsonMapper jsonMapper;
    private final AccuracyService accuracyService;
    private final OfferFeedbackService offerFeedbackService;

    @RabbitListener(queues = RabbitConfig.EVENTS_QUEUE)
    public void onEvent(Message message) {
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        log.info("Received {}", routingKey);

        JsonNode payload = jsonMapper.readTree(body).get("payload");

        switch (routingKey) {
            case RabbitConfig.SEGMENT_CHANGED -> {
                SegmentChangedEvent event = jsonMapper.treeToValue(payload, SegmentChangedEvent.class);
                accuracyService.recordMisclassification(
                        event.campaignNo(), event.originalSegment(), event.correctedSegment());
            }
            case RabbitConfig.OFFER_RESPONDED -> {
                OfferRespondedEvent event = jsonMapper.treeToValue(payload, OfferRespondedEvent.class);
                offerFeedbackService.record(event.subscriberRef(), event.response());
            }
            default -> log.warn("No handler for routing key {}", routingKey);
        }
    }
}
