package com.offerhub.gamification.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class RabbitConfig {

    /** Owned by Campaign Service; declared here too so start order does not matter. */
    public static final String EVENTS_EXCHANGE = "offerhub.events";

    /** Durable: points must survive this service being down when an event is published. */
    public static final String EVENTS_QUEUE = "gamification.events";

    public static final String CAMPAIGN_OPTIMIZED = "campaign.optimized";
    public static final String SLA_BREACHED = "sla.breached";

    /**
     * The JSON converter is set on the template only, not registered as a global bean.
     * A global one would also be used by the listener container, which would then try to
     * instantiate the publisher's class named in the message headers - a class this
     * service does not have, and should not have. Incoming messages stay raw JSON.
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, JsonMapper jsonMapper) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new JacksonJsonMessageConverter(jsonMapper));
        return template;
    }

    @Bean
    public TopicExchange offerhubEventsExchange() {
        return new TopicExchange(EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public Queue gamificationEventsQueue() {
        return new Queue(EVENTS_QUEUE, true);
    }

    /**
     * Only the two routing keys this service scores on. Campaign publishes more, binding
     * narrowly means a new event type never wakes this service up by accident.
     */
    @Bean
    public Binding campaignOptimizedBinding(Queue gamificationEventsQueue, TopicExchange offerhubEventsExchange) {
        return BindingBuilder.bind(gamificationEventsQueue).to(offerhubEventsExchange).with(CAMPAIGN_OPTIMIZED);
    }

    @Bean
    public Binding slaBreachedBinding(Queue gamificationEventsQueue, TopicExchange offerhubEventsExchange) {
        return BindingBuilder.bind(gamificationEventsQueue).to(offerhubEventsExchange).with(SLA_BREACHED);
    }
}
