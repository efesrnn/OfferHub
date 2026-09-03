package com.offerhub.gamification.event;

import com.offerhub.gamification.service.LeaderboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Moves the ranking after the ledger has committed, never before.
 *
 * Redis is not transactional and takes no part in a rollback. Incrementing it while the
 * scoring transaction is still open means a transaction that fails afterwards leaves the
 * leaderboard holding points the ledger never recorded - and Postgres is meant to be the
 * source of truth here, with Redis derived from it.
 *
 * A failure to reach Redis does not undo the points: they are committed and correct, only
 * the ranking is stale. The rebuild from point_entries is the repair for exactly that.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LeaderboardUpdater {

    private final LeaderboardService leaderboardService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPointsAwarded(PointsAwarded event) {
        try {
            leaderboardService.addPoints(event.expertId(), event.points());
        } catch (RuntimeException ex) {
            log.error("Could not update the leaderboard for {} - the points are recorded, "
                    + "the ranking is behind until it is rebuilt", event.expertId(), ex);
        }
    }
}
