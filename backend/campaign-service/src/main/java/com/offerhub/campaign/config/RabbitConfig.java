package com.offerhub.campaign.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class RabbitConfig {

    public static final String EVENTS_EXCHANGE = "offerhub.events";

    /** Declared on startup, so publishing never depends on a consumer having run first. */
    @Bean
    public TopicExchange offerhubEventsExchange() {
        return new TopicExchange(EVENTS_EXCHANGE, true, false);
    }

     
    @Bean
    public MessageConverter rabbitMessageConverter(JsonMapper jsonMapper) {
        return new JacksonJsonMessageConverter(jsonMapper);
    }
}
