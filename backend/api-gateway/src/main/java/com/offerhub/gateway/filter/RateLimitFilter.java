package com.offerhub.gateway.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

/**
 * Brute force defence for the endpoints reachable without a token, case document 11.
 *
 * A fixed window counter in Redis: one key per client and window, incremented on each
 * request and expired when the window closes. Redis rather than memory because the count
 * has to hold across gateway instances - an attacker should not get a fresh allowance by
 * being routed elsewhere.
 *
 * Written here rather than using the built in RequestRateLimiter, which rejects by
 * completing the response with no body at all. Every other error this system returns is
 * the {success, data, error} envelope, and a client cannot parse a status code alone.
 *
 * Counted per client IP, not per user: the attacks this defends against - guessing a
 * password, hammering the OTP endpoint - happen before anyone is authenticated, so there
 * is no user to key on.
 */
@Component
public class RateLimitFilter implements GlobalFilter, Ordered {

    private static final String KEY_PREFIX = "ratelimit";
    private static final String UNKNOWN_CLIENT = "unknown";

    private static final String BODY = """
            {"success":false,"data":null,"error":{"code":"RATE_LIMITED",\
            "message":"Too many requests, slow down"}}""";

    private final ReactiveStringRedisTemplate redis;
    private final List<String> protectedPaths;
    private final int maxRequests;
    private final Duration window;

    public RateLimitFilter(ReactiveStringRedisTemplate redis,
                           @Value("${ratelimit.paths}") String paths,
                           @Value("${ratelimit.requests}") int maxRequests,
                           @Value("${ratelimit.window-seconds}") int windowSeconds) {
        this.redis = redis;
        this.protectedPaths = List.of(paths.split(","));
        this.maxRequests = maxRequests;
        this.window = Duration.ofSeconds(windowSeconds);
    }

    /** Before authentication: a throttled request should not cost a token verification. */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE - 1;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (protectedPaths.stream().noneMatch(path::startsWith)) {
            return chain.filter(exchange);
        }

        return redis.opsForValue().increment(keyFor(exchange))
                .flatMap(count -> count > maxRequests
                        ? reject(exchange)
                        : expireOnFirst(exchange, count).then(chain.filter(exchange)))
                // Redis being unreachable must not close the gate on everybody. Failing
                // open is the right trade here: the alternative is that a cache outage
                // takes down login for the whole system.
                .onErrorResume(error -> chain.filter(exchange));
    }

    /**
     * The window starts with its first request. Only that one sets the expiry, so a busy
     * client cannot keep pushing the deadline out and never reset.
     */
    private Mono<Boolean> expireOnFirst(ServerWebExchange exchange, Long count) {
        return count == 1 ? redis.expire(keyFor(exchange), window) : Mono.just(true);
    }

    /** Key changes when the window does, so old counters simply stop being read. */
    private String keyFor(ServerWebExchange exchange) {
        long windowIndex = System.currentTimeMillis() / window.toMillis();
        return "%s:%s:%d".formatted(KEY_PREFIX, clientIp(exchange), windowIndex);
    }

    private static String clientIp(ServerWebExchange exchange) {
        return exchange.getRequest().getRemoteAddress() == null
                ? UNKNOWN_CLIENT
                : exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
    }

    private static Mono<Void> reject(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        DataBuffer buffer = response.bufferFactory().wrap(BODY.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
