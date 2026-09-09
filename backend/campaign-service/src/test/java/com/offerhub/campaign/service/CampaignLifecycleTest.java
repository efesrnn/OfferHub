package com.offerhub.campaign.service;

import com.offerhub.campaign.dto.CreateCampaignRequest;
import com.offerhub.campaign.entity.Campaign;
import com.offerhub.campaign.entity.CampaignStatus;
import com.offerhub.campaign.entity.CampaignType;
import com.offerhub.campaign.entity.Priority;
import com.offerhub.campaign.entity.Segment;
import com.offerhub.campaign.repository.CampaignRepository;
import com.offerhub.campaign.security.CallerIdentity;
import com.offerhub.campaign.security.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * The campaign's own status, which is decided outside the case state machine and so is not
 * covered by CaseStateMachineTest. A campaign used to be stranded in YENI whenever AI scored
 * it well enough that no case was opened, because only a case could move it on.
 */
@ExtendWith(MockitoExtension.class)
class CampaignLifecycleTest {

    private static final CallerIdentity EXPERT =
            new CallerIdentity(UUID.randomUUID(), Role.EXPERT);

    @Mock private CampaignRepository campaignRepository;
    @Mock private CampaignNumberGenerator numberGenerator;
    @Mock private OptimizationCaseService caseService;
    @Mock private CampaignAiAdvisor aiAdvisor;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private CampaignService campaignService;

    /** The regression: a well scored campaign has no case, so nothing else can publish it. */
    @Test
    void publishesImmediatelyWhenNoCaseIsOpened() {
        scoredAt("0.75");
        when(caseService.openIfLowConversion(any(Campaign.class))).thenReturn(false);

        assertThat(create().status()).isEqualTo(CampaignStatus.YAYINDA);
    }

    /** YENI is the expert's queue, so a campaign with a case has to stay in it. */
    @Test
    void staysInYeniWhileACaseIsOpen() {
        scoredAt("0.42");
        when(caseService.openIfLowConversion(any(Campaign.class))).thenReturn(true);

        assertThat(create().status()).isEqualTo(CampaignStatus.YENI);
    }

    /** AI unreachable is the fallback path: unscored, so it goes to an expert like a low score. */
    @Test
    void staysInYeniWhenAiWasUnavailable() {
        when(aiAdvisor.scoreFor(any(Segment.class), any(CampaignType.class)))
                .thenReturn(CampaignScoring.unavailable());
        when(caseService.openIfLowConversion(any(Campaign.class))).thenReturn(true);

        assertThat(create().status()).isEqualTo(CampaignStatus.YENI);
    }

    /** The other half of the fix: without a case, only expiry can retire a campaign. */
    @Test
    void archivesExpiredCampaignsThatHaveNoCase() {
        Campaign expired = Campaign.builder()
                .campaignNo("CMP-2026-000002")
                .status(CampaignStatus.YAYINDA)
                .validUntil(Instant.now().minus(1, ChronoUnit.DAYS))
                .build();
        when(campaignRepository.findExpiredWithoutCase(any(Instant.class)))
                .thenReturn(List.of(expired));

        assertThat(campaignService.archiveExpiredWithoutCase()).isEqualTo(1);
        assertThat(expired.getStatus()).isEqualTo(CampaignStatus.ARSIVLENDI);
    }

    private void scoredAt(String probability) {
        BigDecimal score = new BigDecimal(probability);
        when(aiAdvisor.scoreFor(eq(Segment.YUKSEK_DEGER), eq(CampaignType.EK_PAKET)))
                .thenReturn(new CampaignScoring(Segment.YUKSEK_DEGER, score, score, Priority.ORTA));
    }

    /** Stubbed here rather than in a @BeforeEach: the archival test never saves anything. */
    private com.offerhub.campaign.dto.CampaignResponse create() {
        when(numberGenerator.next()).thenReturn("CMP-2026-000001");
        when(campaignRepository.saveAndFlush(any(Campaign.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        return campaignService.create(new CreateCampaignRequest(
                "Test", CampaignType.EK_PAKET, Segment.YUKSEK_DEGER, 20,
                Instant.now().plus(30, ChronoUnit.DAYS)), EXPERT);
    }
}
