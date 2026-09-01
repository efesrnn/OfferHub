package com.offerhub.gamification.service;

import com.offerhub.gamification.exception.ApiException;
import com.offerhub.gamification.exception.ErrorCode;

import java.util.Arrays;

/** Leaderboard windows the contract exposes: ?period=daily or ?period=weekly. */
public enum Period {

    DAILY("daily"),
    WEEKLY("weekly");

    private final String param;

    Period(String param) {
        this.param = param;
    }

    public String getParam() {
        return param;
    }

    /** Parsed by hand because the query value is lower case and the enum name is not. */
    public static Period fromParam(String value) {
        return Arrays.stream(values())
                .filter(period -> period.param.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new ApiException(ErrorCode.VALIDATION_ERROR,
                        "period must be daily or weekly"));
    }
}
