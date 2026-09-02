package com.offerhub.identity.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.offerhub.identity.dto.ApiResponse;
import com.offerhub.identity.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Token hic yoksa buraya duser - ayni sekilde audit'e yazilir, mevcut 403 davranisi korunur. */
@Component
@RequiredArgsConstructor
public class AuditingAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
            throws IOException {
        auditLogService.record("anonymous", "UNAUTHORIZED_ACCESS", "FAILED",
                ClientIpResolver.resolve(request), request.getMethod() + " " + request.getRequestURI());

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.getWriter().write(objectMapper.writeValueAsString(
                ApiResponse.error("FORBIDDEN", "Kimlik dogrulama gerekli")));
    }
}
