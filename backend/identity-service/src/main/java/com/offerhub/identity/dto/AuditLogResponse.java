package com.offerhub.identity.dto;

import com.offerhub.identity.entity.AuditLog;

public record AuditLogResponse(
        String id,
        String userId,
        String action,
        String timestamp,
        String ip,
        String result,
        String detail
) {
    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(
                log.getId().toString(),
                log.getUserId(),
                log.getAction(),
                log.getTimestamp().toString(),
                log.getIpAddress(),
                log.getResult(),
                log.getDetail());
    }
}
