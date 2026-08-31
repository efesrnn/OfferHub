package com.offerhub.campaign.config;

import com.offerhub.campaign.security.CallerIdentityArgumentResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/** Registers the resolver that turns the gateway's identity headers into a CallerIdentity. */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final CallerIdentityArgumentResolver callerIdentityArgumentResolver;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(callerIdentityArgumentResolver);
    }
}
