package com.offerhub.identity.service;

import com.offerhub.identity.dto.AuditLogResponse;
import com.offerhub.identity.dto.PagedResponse;
import com.offerhub.identity.entity.AuditLog;
import com.offerhub.identity.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private static final int MAX_PAGE_SIZE = 100;

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void record(String userId, String action, String result, String ipAddress, String detail) {
        auditLogRepository.save(AuditLog.builder()
                .userId(userId)
                .action(action)
                .result(result)
                .ipAddress(ipAddress)
                .detail(detail)
                .build());
    }

    @Transactional(readOnly = true)
    public PagedResponse<AuditLogResponse> search(String actionQuery, String action, String result,
                                                    String fromDate, String toDate, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE));

        Page<AuditLog> found = auditLogRepository.search(
                blankToNull(actionQuery),
                blankToNull(action),
                blankToNull(result),
                parseStartOfDay(fromDate),
                parseStartOfNextDay(toDate),
                pageable);

        return new PagedResponse<>(
                found.getContent().stream().map(AuditLogResponse::from).toList(),
                found.getTotalElements(),
                page,
                size);
    }

    private static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value : null;
    }

    private static Instant parseStartOfDay(String date) {
        if (!StringUtils.hasText(date)) return null;
        return LocalDate.parse(date).atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private static Instant parseStartOfNextDay(String date) {
        if (!StringUtils.hasText(date)) return null;
        return LocalDate.parse(date).plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    }
}
