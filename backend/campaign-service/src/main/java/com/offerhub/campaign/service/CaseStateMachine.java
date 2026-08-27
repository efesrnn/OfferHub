package com.offerhub.campaign.service;

import com.offerhub.campaign.entity.CaseStatus;
import com.offerhub.campaign.exception.ApiException;
import com.offerhub.campaign.exception.ErrorCode;

import java.util.Map;
import java.util.Set;

import static com.offerhub.campaign.entity.CaseStatus.ARSIVLENDI;
import static com.offerhub.campaign.entity.CaseStatus.ATANDI;
import static com.offerhub.campaign.entity.CaseStatus.OPTIMIZE_EDILIYOR;
import static com.offerhub.campaign.entity.CaseStatus.TAMAMLANDI;
import static com.offerhub.campaign.entity.CaseStatus.TEST_EDILIYOR;
import static com.offerhub.campaign.entity.CaseStatus.YAYINDA;
import static com.offerhub.campaign.entity.CaseStatus.YENI;

/**
 * TODO: who may perform a transition (expert/supervisor/system) once JWT roles land.
 */
public final class CaseStateMachine {

    private static final Map<CaseStatus, Set<CaseStatus>> ALLOWED = Map.of(
            YENI, Set.of(ATANDI),
            ATANDI, Set.of(OPTIMIZE_EDILIYOR),
            OPTIMIZE_EDILIYOR, Set.of(TEST_EDILIYOR, TAMAMLANDI),
            TEST_EDILIYOR, Set.of(OPTIMIZE_EDILIYOR),
            TAMAMLANDI, Set.of(YAYINDA),
            YAYINDA, Set.of(ARSIVLENDI),
            ARSIVLENDI, Set.of());

    private CaseStateMachine() {
    }

    public static boolean isAllowed(CaseStatus from, CaseStatus to) {
        return ALLOWED.getOrDefault(from, Set.of()).contains(to);
    }

    /** @throws ApiException INVALID_STATE_TRANSITION (422) when the move is not in the table */
    public static void assertAllowed(CaseStatus from, CaseStatus to) {
        if (!isAllowed(from, to)) {
            throw new ApiException(ErrorCode.INVALID_STATE_TRANSITION,
                    "Cannot move from %s to %s".formatted(from, to));
        }
    }
}
