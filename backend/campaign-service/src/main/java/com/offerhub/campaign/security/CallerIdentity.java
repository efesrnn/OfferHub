package com.offerhub.campaign.security;

import com.offerhub.campaign.exception.ApiException;
import com.offerhub.campaign.exception.ErrorCode;

import java.util.Arrays;
import java.util.UUID;

/**
 * Who is making this request, as established by the gateway. Campaign never parses the
 * token itself - it trusts the two headers, which only the gateway can set.
 */
public record CallerIdentity(UUID userId, Role role) {

    /** @throws ApiException FORBIDDEN when the caller's role is not one of the allowed ones */
    public void requireAnyOf(Role... allowed) {
        if (Arrays.stream(allowed).noneMatch(candidate -> candidate == role)) {
            throw new ApiException(ErrorCode.FORBIDDEN,
                    "Role %s cannot perform this operation".formatted(role));
        }
    }

    public boolean isExpert() {
        return role == Role.EXPERT;
    }
}
