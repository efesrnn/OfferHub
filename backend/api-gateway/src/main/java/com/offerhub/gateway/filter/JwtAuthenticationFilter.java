package com.offerhub.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Validates the access token once, at the edge, and tells the services downstream who the
 * caller is. Services then read two headers instead of parsing the token again.
 */
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    /** Identity owns these paths and they are reached before a token exists. */
    private static final List<String> PUBLIC_PREFIXES = List.of("/api/v1/auth/");

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_ROLE_HEADER = "X-User-Role";

    private final SecretKey key;

    public JwtAuthenticationFilter(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Drop caller supplied identity headers first. Without this anyone could send
        // X-User-Id and be trusted downstream - only this filter may set them.
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove(USER_ID_HEADER);
                    headers.remove(USER_ROLE_HEADER);
                })
                .build();

        if (isPublic(request.getURI().getPath())) {
            return chain.filter(exchange.mutate().request(request).build());
        }

        String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return reject(exchange, "TOKEN_INVALID", "Missing bearer token");
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(authorization.substring(BEARER_PREFIX.length()))
                    .getPayload();

            // A refresh token is signed with the same key, so only the type claim separates them.
            if (!"access".equals(claims.get("type", String.class))) {
                return reject(exchange, "TOKEN_INVALID", "Refresh token cannot be used for API calls");
            }

            String userId = claims.getSubject();
            String role = claims.get("role", String.class);

            // A correct signature is not the same as a usable token. Both claims are
            // required downstream, and building a header from a null would fail inside
            // this filter - which would answer a malformed token with 500 instead of 401.
            if (userId == null || role == null) {
                return reject(exchange, "TOKEN_INVALID", "Token is missing the subject or role claim");
            }

            ServerHttpRequest authenticated = request.mutate()
                    .header(USER_ID_HEADER, userId)
                    .header(USER_ROLE_HEADER, role)
                    .build();

            return chain.filter(exchange.mutate().request(authenticated).build());

        } catch (ExpiredJwtException ex) {
            return reject(exchange, "TOKEN_EXPIRED", "Access token has expired");
        } catch (JwtException | IllegalArgumentException ex) {
            return reject(exchange, "TOKEN_INVALID", "Invalid authentication token");
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private static boolean isPublic(String path) {
        return PUBLIC_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private static Mono<Void> reject(ServerWebExchange exchange, String code, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = "{\"success\":false,\"data\":null,\"error\":{\"code\":\"%s\",\"message\":\"%s\"}}"
                .formatted(code, message);
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
