package com.offerhub.campaign.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Replaces Spring Boot's default chain, which locked every endpoint behind basic auth.
 * Stateless: no session, no form login, nothing kept between requests.
 *
 * permitAll here is not "no authentication" - it is authentication happening somewhere
 * else. The gateway validates the token once at the edge and forwards the caller as the
 * X-User-Id and X-User-Role headers, which it strips from incoming requests first so they
 * cannot be forged. Verifying the signature again in every service would duplicate the
 * secret and the logic for nothing.
 *
 * Authorization stays here, where the rules live: CallerIdentityArgumentResolver rejects a
 * request that arrives without those headers - which is what a request sent straight to
 * this service's port looks like - and each controller checks the role matrix, with the
 * ownership checks in the services on top.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}