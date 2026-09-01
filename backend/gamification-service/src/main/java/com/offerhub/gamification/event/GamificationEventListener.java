package com.offerhub.gamification.event;

import com.offerhub.gamification.config.RabbitConfig;
import com.offerhub.gamification.service.ScoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;

/**
 * Reads the raw JSON rather than letting the converter map it to a class. The publisher
 * writes its own type name into the message headers, and that class does not exist here -
 * parsing the body ourselves keeps the two services independent, which was the point of
 * choosing JSON on the wire.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GamificationEventListener {

    private final JsonMapper jsonMapper;
    private final ScoringService scoringService;

    @RabbitListener(queues = RabbitConfig.EVENTS_QUEUE)
    public void onEvent(Message message) {
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        log.info("Received {}", routingKey);

        JsonNode payload = jsonMapper.readTree(body).get("payload");

        switch (routingKey) {
            case RabbitConfig.CAMPAIGN_OPTIMIZED ->
                    scoringService.score(jsonMapper.treeToValue(payload, CampaignOptimizedEvent.class));
            case RabbitConfig.SLA_BREACHED ->
                    scoringService.score(jsonMapper.treeToValue(payload, SlaBreachedEvent.class));
            default -> log.warn("No handler for routing key {}", routingKey);
        }
    }
}
