package com.offerhub.identity.dto;

import java.util.List;

public record PagedResponse<T>(List<T> items, long total, int page, int size) {
}
