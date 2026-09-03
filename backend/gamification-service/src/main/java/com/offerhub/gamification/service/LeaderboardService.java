package com.offerhub.gamification.service;

import com.offerhub.gamification.dto.LeaderboardEntry;
import com.offerhub.gamification.repository.PointEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Ranking lives in Redis sorted sets: one key per period window, expert id as member,
 * points as score. Ranking is exactly what a sorted set does, so there is no query to
 * write and no table to sort on every page load.
 * Postgres stays the source of truth - point_entries can rebuild any of these keys.
 */
@Service
@RequiredArgsConstructor
public class LeaderboardService {

    private static final String KEY_PREFIX = "leaderboard";

    /** Windows are dated, so a key stops being written to on its own when the day rolls. */
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);

    /**
     * Built from the ISO week fields rather than an "ww" pattern. A pattern reads the week
     * rules from the JVM's default locale, so the same instant would land in different week
     * numbers on a Turkish laptop and in a container running under the C locale - splitting
     * one week's leaderboard across two Redis keys.
     */
    private static final DateTimeFormatter WEEK = new DateTimeFormatterBuilder()
            .appendValue(IsoFields.WEEK_BASED_YEAR, 4)
            .appendLiteral("-W")
            .appendValue(IsoFields.WEEK_OF_WEEK_BASED_YEAR, 2)
            .toFormatter(Locale.ROOT)
            .withZone(ZoneOffset.UTC);

    /** Kept a little past their window so a late reader still sees yesterday's board. */
    private static final Duration DAILY_TTL = Duration.ofDays(2);
    private static final Duration WEEKLY_TTL = Duration.ofDays(14);

    private final StringRedisTemplate redis;
    private final PointEntryRepository pointEntryRepository;

    /** Called on every scored entry; negative points work the same way. */
    public void addPoints(UUID expertId, int points) {
        Instant now = Instant.now();
        increment(key(Period.DAILY, now), expertId, points, DAILY_TTL);
        increment(key(Period.WEEKLY, now), expertId, points, WEEKLY_TTL);
    }

    public List<LeaderboardEntry> top(Period period, int limit) {
        Set<ZSetOperations.TypedTuple<String>> rows =
                redis.opsForZSet().reverseRangeWithScores(key(period, Instant.now()), 0, limit - 1L);

        List<LeaderboardEntry> entries = new ArrayList<>();
        if (rows == null) {
            return entries;
        }

        int rank = 1;
        for (ZSetOperations.TypedTuple<String> row : rows) {
            entries.add(new LeaderboardEntry(rank++, UUID.fromString(row.getValue()),
                    null, row.getScore() == null ? 0 : row.getScore().intValue()));
        }
        return entries;
    }

    /** Null when the expert scored nothing in this window - they are on no board. */
    public Long rankOf(Period period, UUID expertId) {
        Long zeroBased = redis.opsForZSet().reverseRank(key(period, Instant.now()), expertId.toString());
        return zeroBased == null ? null : zeroBased + 1;
    }

    /**
     * Rebuilds both windows from the ledger.
     *
     * Redis is a derived store here: it holds the ranking, Postgres holds the reason for
     * every point in it. That is what makes a flushed or restarted Redis an inconvenience
     * rather than lost work - and it is also the repair for the drift that came from the
     * leaderboard being wired up after the scoring engine, so older entries were never
     * counted into any key.
     *
     * The keys are deleted first rather than incremented into: rebuilding on top of
     * existing scores would double every point that survived.
     */
    public int rebuild() {
        Instant now = Instant.now();
        int restored = 0;

        restored += rebuildWindow(Period.DAILY, startOfDay(now), now, DAILY_TTL);
        restored += rebuildWindow(Period.WEEKLY, startOfWeek(now), now, WEEKLY_TTL);
        return restored;
    }

    private int rebuildWindow(Period period, Instant since, Instant now, Duration ttl) {
        String key = key(period, now);
        redis.delete(key);

        List<Object[]> totals = pointEntryRepository.totalsSince(since);
        for (Object[] row : totals) {
            UUID expertId = (UUID) row[0];
            long points = ((Number) row[1]).longValue();
            redis.opsForZSet().add(key, expertId.toString(), points);
        }

        if (!totals.isEmpty()) {
            redis.expire(key, ttl);
        }
        return totals.size();
    }

    /** Windows are UTC, matching the keys they are written under. */
    private static Instant startOfDay(Instant at) {
        return at.atZone(ZoneOffset.UTC).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private static Instant startOfWeek(Instant at) {
        return at.atZone(ZoneOffset.UTC).toLocalDate()
                .with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                .atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private void increment(String key, UUID expertId, int points, Duration ttl) {
        redis.opsForZSet().incrementScore(key, expertId.toString(), points);
        redis.expire(key, ttl);
    }

    private static String key(Period period, Instant at) {
        DateTimeFormatter formatter = period == Period.DAILY ? DAY : WEEK;
        return "%s:%s:%s".formatted(KEY_PREFIX, period.getParam(), formatter.format(at));
    }
}
