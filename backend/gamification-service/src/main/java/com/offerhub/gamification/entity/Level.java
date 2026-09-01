package com.offerhub.gamification.entity;

/** Levels from the case document, section 7.3. Derived from total points, never stored. */
public enum Level {

    BRONZ(0),
    GUMUS(500),
    ALTIN(1500),
    PLATIN(3000);

    private final int minPoints;

    Level(int minPoints) {
        this.minPoints = minPoints;
    }

    /** Walks down from the highest level so a new level only needs one line here. */
    public static Level fromPoints(int totalPoints) {
        Level[] levels = values();
        for (int i = levels.length - 1; i >= 0; i--) {
            if (totalPoints >= levels[i].minPoints) {
                return levels[i];
            }
        }
        return BRONZ;
    }
}
