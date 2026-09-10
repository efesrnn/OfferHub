package com.offerhub.campaign.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Case document 9 asks for Swagger on Campaign.
 *
 * The two gateway headers are declared as the security scheme because that is genuinely how
 * this service identifies a caller: the gateway verifies the JWT and forwards the result as
 * headers, so anyone reading this page has to know a direct call carries them itself. Filling
 * them into the Authorize dialog also makes "Try it out" work against port 8082.
 */
@Configuration
public class OpenApiConfig {

    private static final String USER_ID = "X-User-Id";
    private static final String USER_ROLE = "X-User-Role";

    @Bean
    public OpenAPI campaignOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("OfferHub Campaign Service")
                        .version("v1")
                        .description("""
                                Campaign lifecycle, optimization cases, SLA tracking and subscriber offers.

                                Authentication happens at the API Gateway. It verifies the JWT and passes the
                                caller down as X-User-Id and X-User-Role, and this service authorizes on those
                                two headers. That is why its own Spring Security chain is permitAll: the
                                service is not reachable from outside the compose network.

                                Every response uses the shared envelope {success, data, error}. On failure
                                error.code carries a stable string such as INVALID_STATE_TRANSITION, and a
                                case transition outside the state machine of section 5.2 returns 422."""))
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
