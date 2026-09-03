package com.offerhub.gateway.error;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * Puts the gateway's own errors into the envelope every service already answers with.
 *
 * Without this a request to a path no route matches falls through to the default error
 * handling and comes back in a different shape: a client that parses {success, data,
 * error} everywhere else suddenly gets {timestamp, path, status}. Rate limit rejections
 * had the same problem - the filter returns a bare 429 with no body at all.
 *
 * Implemented against WebExceptionHandler from spring-web rather than Boot's error
 * machinery, which moves between versions; this interface has not.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GatewayErrorHandler implements WebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable error) {
        ServerHttpResponse response = exchange.getResponse();

        // Something already started writing - the status is on its way out and the body
        // is no longer ours to replace.
        if (response.isCommitted()) {
            return Mono.error(error);
        }

        HttpStatus status = statusOf(error);
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = """
                {"success":false,"data":null,"error":{"code":"%s","message":"%s"}}"""
                .formatted(codeFor(status), messageFor(status));

        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    /**
     * An unmatched route and a rejected rate limit both arrive as ResponseStatusException;
     * anything else is a genuine fault and reported as one.
     */
    private static HttpStatus statusOf(Throwable error) {
        if (error instanceof ResponseStatusException statusException) {
            HttpStatus resolved = HttpStatus.resolve(statusException.getStatusCode().value());
            return resolved == null ? HttpStatus.INTERNAL_SERVER_ERROR : resolved;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    /** The catalog in docs/ERROR-CODES.md, so the gateway speaks the same vocabulary. */
    private static String codeFor(HttpStatus status) {
        return switch (status) {
            case NOT_FOUND -> "NOT_FOUND";
            case TOO_MANY_REQUESTS -> "RATE_LIMITED";
            case SERVICE_UNAVAILABLE, GATEWAY_TIMEOUT, BAD_GATEWAY -> "SERVICE_UNAVAILABLE";
            case FORBIDDEN -> "FORBIDDEN";
            case METHOD_NOT_ALLOWED -> "VALIDATION_ERROR";
            default -> "INTERNAL_ERROR";
        };
    }

    private static String messageFor(HttpStatus status) {
        return switch (status) {
            case NOT_FOUND -> "No route matches this path";
            case TOO_MANY_REQUESTS -> "Too many requests, slow down";
            case SERVICE_UNAVAILABLE, GATEWAY_TIMEOUT, BAD_GATEWAY -> "The service is not reachable right now";
            case METHOD_NOT_ALLOWED -> "This method is not allowed on that path";
            default -> "Something went wrong at the gateway";
        };
    }
}
