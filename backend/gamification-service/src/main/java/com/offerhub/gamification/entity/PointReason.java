package com.offerhub.gamification.entity;

/** The scoring table from the case document, section 7.1. Points live with the reason. */
public enum PointReason {

    OPTIMIZATION_COMPLETED(10),
    FAST_OPTIMIZATION(5),
    CONVERSION_TARGET_EXCEEDED(15),
    CRITICAL_WITHIN_SLA(15),
    SLA_BREACH(-5),
    LOW_RATING(-3);

    private final int points;

    PointReason(int points) {
        this.points = points;
    }

    public int getPoints() {
        return points;
    }
}
