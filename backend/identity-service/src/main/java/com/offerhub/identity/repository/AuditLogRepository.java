package com.offerhub.identity.repository;

import com.offerhub.identity.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    @Query("""
            select a from AuditLog a
            where (:query is null
                   or lower(a.action) like lower(concat('%', :query, '%'))
                   or lower(a.userId) like lower(concat('%', :query, '%'))
                   or lower(a.ipAddress) like lower(concat('%', :query, '%')))
              and (:action is null or a.action = :action)
              and (:result is null or a.result = :result)
              and (:from is null or a.timestamp >= :from)
              and (:to is null or a.timestamp < :to)
            order by a.timestamp desc
            """)
    Page<AuditLog> search(@Param("query") String query,
                          @Param("action") String action,
                          @Param("result") String result,
                          @Param("from") Instant from,
                          @Param("to") Instant to,
                          Pageable pageable);
}
