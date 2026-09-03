package com.offerhub.identity.security;

import jakarta.servlet.http.HttpServletRequest;

/** Gateway'in ilettigi X-Forwarded-For'u tercih eder, yoksa dogrudan remote adrese duser. */
public final class ClientIpResolver {

    private ClientIpResolver() {
    }

    public static String resolve(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
