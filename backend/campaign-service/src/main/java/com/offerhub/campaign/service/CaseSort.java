package com.offerhub.campaign.service;

import com.offerhub.campaign.exception.ApiException;
import com.offerhub.campaign.exception.ErrorCode;

/**
 * Ordering of the case list. Two different questions: PRIORITY answers "what matters
 * most", SLA answers "what runs out first" - a DUSUK case an hour from its deadline is
 * more urgent today than a KRITIK one opened a minute ago.
 */
public enum CaseSort {

    PRIORITY,
    SLA;

    public static CaseSort fromParam(String param) {
        if (param == null || param.isBlank()) {
            return PRIORITY;
        }
        try {
            return valueOf(param.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "sort must be 'priority' or 'sla'");
        }
    }
}
