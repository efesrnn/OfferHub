package com.offerhub.campaign.service;

import com.offerhub.campaign.entity.CaseStatus;
import com.offerhub.campaign.exception.ApiException;
import com.offerhub.campaign.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static com.offerhub.campaign.entity.CaseStatus.ARSIVLENDI;
import static com.offerhub.campaign.entity.CaseStatus.ATANDI;
import static com.offerhub.campaign.entity.CaseStatus.OPTIMIZE_EDILIYOR;
import static com.offerhub.campaign.entity.CaseStatus.TAMAMLANDI;
import static com.offerhub.campaign.entity.CaseStatus.TEST_EDILIYOR;
import static com.offerhub.campaign.entity.CaseStatus.YAYINDA;
import static com.offerhub.campaign.entity.CaseStatus.YENI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** No Spring context - the state machine is plain Java, so the test is instant. */
class CaseStateMachineTest {

    @Test
    void allowsEveryTransitionInTheContract() {
        assertThat(CaseStateMachine.isAllowed(YENI, ATANDI)).isTrue();
        assertThat(CaseStateMachine.isAllowed(ATANDI, OPTIMIZE_EDILIYOR)).isTrue();
        assertThat(CaseStateMachine.isAllowed(OPTIMIZE_EDILIYOR, TEST_EDILIYOR)).isTrue();
        assertThat(CaseStateMachine.isAllowed(TEST_EDILIYOR, OPTIMIZE_EDILIYOR)).isTrue();
        assertThat(CaseStateMachine.isAllowed(OPTIMIZE_EDILIYOR, TAMAMLANDI)).isTrue();
        assertThat(CaseStateMachine.isAllowed(TAMAMLANDI, YAYINDA)).isTrue();
        assertThat(CaseStateMachine.isAllowed(YAYINDA, ARSIVLENDI)).isTrue();
    }

    /** The contract says every move outside the table is rejected - so count them. */
    @Test
    void rejectsEveryOtherTransition() {
        long allowed = 0;
        for (CaseStatus from : CaseStatus.values()) {
            for (CaseStatus to : CaseStatus.values()) {
                if (CaseStateMachine.isAllowed(from, to)) {
                    allowed++;
                }
            }
        }
        assertThat(allowed).isEqualTo(7);
    }

    @Test
    void staysPutOnSelfTransition() {
        assertThat(CaseStateMachine.isAllowed(ATANDI, ATANDI)).isFalse();
    }

    @Test
    void archivedIsTerminal() {
        for (CaseStatus to : CaseStatus.values()) {
            assertThat(CaseStateMachine.isAllowed(ARSIVLENDI, to)).isFalse();
        }
    }

    @Test
    void assertAllowedThrowsInvalidStateTransition() {
        assertThatThrownBy(() -> CaseStateMachine.assertAllowed(YENI, TAMAMLANDI))
                .isInstanceOf(ApiException.class)
                .hasMessage("Cannot move from YENI to TAMAMLANDI")
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo(ErrorCode.INVALID_STATE_TRANSITION);
    }

    @Test
    void assertAllowedIsSilentOnAValidMove() {
        CaseStateMachine.assertAllowed(YENI, ATANDI);
    }
}
