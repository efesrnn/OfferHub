package com.offerhub.campaign.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Paged list payload (page + size).
 * { "items": [...], "total": 142, "page": 0, "size": 20 }
 */
public record PagedResult<T>(List<T> items, long total, int page, int size) {

    public static <T> PagedResult<T> from(Page<T> page) {
        return new PagedResult<>(page.getContent(), page.getTotalElements(), page.getNumber(), page.getSize());
    }
}
