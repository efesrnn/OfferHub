package com.offerhub.identity.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.offerhub.identity.dto.ApiResponse;
import com.offerhub.identity.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Rol uyusmuyorsa (403) buraya duser - case Bolum 4.3/4.4: bu denemeler audit log'a yazilmali. */
@Component
@RequiredArgsConstructor
public class AuditingAccessDeniedHandler implements AccessDeniedHandler {

    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
            throws IOException {
        String userId = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getName()
                : "anonymous";

        auditLogService.record(userId, "UNAUTHORIZED_ACCESS", "FAILED",
                ClientIpResolver.resolve(request), request.getMethod() + " " + request.getRequestURI());

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.getWriter().write(objectMapper.writeValueAsString(
                ApiResponse.error("FORBIDDEN", "Bu islem icin yetkiniz yok")));
    }
}
