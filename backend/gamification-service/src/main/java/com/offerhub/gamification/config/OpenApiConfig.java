package com.offerhub.gamification.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Same shape as the Campaign one, for the same reason: the two gateway headers are how this
 * service identifies a caller, so leaving them out would make the page read as an
 * unauthenticated service. Filling them into the Authorize dialog makes "Try it out" work
 * against port 8084.
 */
@Configuration
public class OpenApiConfig {

    private static final String USER_ID = "X-User-Id";
    private static final String USER_ROLE = "X-User-Role";

    @Bean
    public OpenAPI gamificationOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("OfferHub Gamification Service")
                        .version("v1")
                        .description("""
                                Points, badges, levels and the leaderboard for campaign experts.

                                This service is driven by events, not by calls. Campaign publishes
                                campaign.optimized, sla.breached and offer.rated onto RabbitMQ, and
                                everything below is read from what those produced. Nothing here
                                writes points, so a client cannot award itself anything.

                                Authentication happens at the API Gateway, which verifies the JWT and
                                passes the caller down as X-User-Id and X-User-Role. Profile and
                                badges always answer for the caller in those headers, with no id in
                                any path, so there is no record to address but your own.

                                Every response uses the shared envelope {success, data, error}.""")
                )
                .components(new Components()
                        .addSecuritySchemes(USER_ID, headerScheme(USER_ID))
                        .addSecuritySchemes(USER_ROLE, headerScheme(USER_ROLE)))
                .addSecurityItem(new SecurityRequirement().addList(USER_ID).addList(USER_ROLE));
    }

    private static SecurityScheme headerScheme(String name) {
        return new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .name(name);
    }
}
