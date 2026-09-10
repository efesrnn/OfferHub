package com.offerhub.identity.repository;

import com.offerhub.identity.entity.AuditLog;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Bir JPQL sorgusu yerine Specification: Postgres/Hibernate, sadece "IS NULL" dalinda
 * kullanilan bir parametrenin SQL tipini guvenilir sekilde cikaramiyor (sessizce bytea'ya
 * dusuyor, sonraki cast de patliyor). Specification bu sinifi sorunu tamamen ortadan
 * kaldiriyor: bos birakilan bir filtre sorguya hic predicate olarak eklenmiyor.
 */
public final class AuditLogSpecifications {

    private AuditLogSpecifications() {
    }

    public static Specification<AuditLog> search(String query, String action, String result,
                                                   Instant from, Instant to) {
        return (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(query)) {
                String like = "%" + query.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("action")), like),
                        cb.like(cb.lower(root.get("userId")), like),
                        cb.like(cb.lower(root.get("ipAddress")), like)));
            }
            if (StringUtils.hasText(action)) {
                predicates.add(cb.equal(root.get("action"), action));
            }
            if (StringUtils.hasText(result)) {
                predicates.add(cb.equal(root.get("result"), result));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThan(root.get("timestamp"), to));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
