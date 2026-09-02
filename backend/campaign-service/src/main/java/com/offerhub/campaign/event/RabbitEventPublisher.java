package com.offerhub.campaign.event;

import com.offerhub.campaign.config.RabbitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * AFTER_COMMIT, not immediately: an event states that something happened. Publishing
     * before the commit means a rollback would leave an announced but undone change -
     * Gamification would hand out points for work that never landed.
     * A failure here does not roll the business change back; the change is already
     * committed and correct. That is the trade-off of publishing after commit.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(OutboundEvent event) {
        EventEnvelope envelope = new EventEnvelope(event.eventType(), Instant.now(), event.payload());

        try {
            // Routing key is the eventType itself, so a new consumer binds without us changing anything.
            rabbitTemplate.convertAndSend(RabbitConfig.EVENTS_EXCHANGE, event.eventType(), envelope);
            log.info("Published {}", event.eventType());
        } catch (RuntimeException ex) {
            // Swallowed on purpose. An exception thrown from an after-commit callback
            // propagates out of the commit, so a broker outage would turn a campaign that
            // was created successfully into a 500 for the caller. The case document
            // requires the rest of the system to keep working when one service is down,
            // and the demo tests exactly that by stopping a container.
            // The cost is a lost announcement, logged loudly: business data stays correct,
            // only the notification is missed. An outbox table would close that gap.
            log.error("Could not publish {} - the change is committed, the event is lost",
                    event.eventType(), ex);
        }
    }
}
