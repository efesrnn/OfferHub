package com.offerhub.campaign.service;

import com.offerhub.campaign.entity.CaseStatus;
import com.offerhub.campaign.exception.ApiException;
import com.offerhub.campaign.exception.ErrorCode;
import com.offerhub.campaign.security.Role;

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
 * The transition table from docs/CAMPAIGN-API.md section 8. Anything not listed here is
 * rejected - that is the whole point, the rules live in one readable place instead of
 * being spread over if statements in the service.
 */
public final class CaseStateMachine {

    /** One move in the case lifecycle. */
    public record Transition(CaseStatus from, CaseStatus to) {
    }

    /**
     * Both dimensions of the contract table in one map: which moves exist, and who may
     * make them. The contract's "Sistem" rows have no scheduler behind them yet, so a
     * supervisor stands in for the system.
     */
    private static final Map<Transition, Set<Role>> ALLOWED = Map.of(
            new Transition(YENI, ATANDI), Set.of(Role.SUPERVISOR),
            new Transition(ATANDI, OPTIMIZE_EDILIYOR), Set.of(Role.EXPERT),
            new Transition(OPTIMIZE_EDILIYOR, TEST_EDILIYOR), Set.of(Role.EXPERT),
            new Transition(TEST_EDILIYOR, OPTIMIZE_EDILIYOR), Set.of(Role.SUPERVISOR),
            new Transition(OPTIMIZE_EDILIYOR, TAMAMLANDI), Set.of(Role.EXPERT),
            new Transition(TAMAMLANDI, YAYINDA), Set.of(Role.SUPERVISOR),
            new Transition(YAYINDA, ARSIVLENDI), Set.of(Role.SUPERVISOR));

    /** Still being worked on. Assigning an expert to a finished case buys nothing. */
    private static final Set<CaseStatus> OPEN = Set.of(YENI, ATANDI, OPTIMIZE_EDILIYOR, TEST_EDILIYOR);

    private CaseStateMachine() {
    }

    public static boolean isOpen(CaseStatus status) {
        return OPEN.contains(status);
    }

    public static boolean isAllowed(CaseStatus from, CaseStatus to) {
        return ALLOWED.containsKey(new Transition(from, to));
    }

    /**
     * Two different rejections on purpose: a move nobody can make is a state error (422),
     * a move this role cannot make is an authorization error (403).
     *
     * @throws ApiException INVALID_STATE_TRANSITION when the move is not in the table
     * @throws ApiException FORBIDDEN when the move exists but not for this role
     */
    public static void assertAllowed(CaseStatus from, CaseStatus to, Role role) {
        Set<Role> allowedRoles = ALLOWED.get(new Transition(from, to));

        if (allowedRoles == null) {
            throw new ApiException(ErrorCode.INVALID_STATE_TRANSITION,
                    "Cannot move from %s to %s".formatted(from, to));
        }
        if (!allowedRoles.contains(role)) {
            throw new ApiException(ErrorCode.FORBIDDEN,
                    "Role %s cannot move a case from %s to %s".formatted(role, from, to));
        }
    }
}
