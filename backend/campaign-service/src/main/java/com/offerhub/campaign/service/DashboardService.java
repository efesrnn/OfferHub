package com.offerhub.campaign.service;

import com.offerhub.campaign.dto.DashboardResponse;
import com.offerhub.campaign.entity.CaseStatus;
import com.offerhub.campaign.entity.OfferStatus;
import com.offerhub.campaign.entity.Segment;
import com.offerhub.campaign.repository.CampaignRepository;
import com.offerhub.campaign.repository.OfferRepository;
import com.offerhub.campaign.repository.OptimizationCaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The supervisor's numbers. Counted on demand rather than kept in a summary table: the
 * data set is small, and a stored total that drifts from the rows it summarises is worse
 * than a query that takes a few milliseconds.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    /** Rates are reported as a 0.00-1.00 fraction; the client renders the percentage. */
    private static final int RATE_SCALE = 2;

    private final CampaignRepository campaignRepository;
    private final OptimizationCaseRepository caseRepository;
    private final OfferRepository offerRepository;

    @Transactional(readOnly = true)
    public DashboardResponse load() {
        long answeredOffers = offerRepository.countByStatusNot(OfferStatus.PENDING);
        long acceptedOffers = offerRepository.countByStatus(OfferStatus.ACCEPTED);

        long casesWithDeadline = caseRepository.countBySlaDeadlineIsNotNull();
        long breachedCases = caseRepository.countBySlaBreachedAtIsNotNull();

        return new DashboardResponse(
                segmentDistribution(),
                ratio(acceptedOffers, answeredOffers),
                // Compliance is the other side of the breach count, so the two cards on the
                // screen can never contradict each other.
                ratio(casesWithDeadline - breachedCases, casesWithDeadline),
                caseRepository.countBySlaBreachedAtIsNotNullAndCompletedAtIsNull(),
                caseRepository.countByStatus(CaseStatus.YENI));
    }

    /**
     * Every segment appears, including the ones at zero: a pie chart that silently drops
     * empty slices makes "no RISKLI_KAYIP campaigns" look the same as "that segment does
     * not exist".
     */
    private Map<Segment, Long> segmentDistribution() {
        Map<Segment, Long> distribution = new EnumMap<>(Segment.class);
        Arrays.stream(Segment.values()).forEach(segment -> distribution.put(segment, 0L));

        for (Object[] row : campaignRepository.countPerSegment()) {
            distribution.put((Segment) row[0], (Long) row[1]);
        }
        return distribution;
    }

    /** Nothing measured yet is reported as zero, not as a division by zero or a null. */
    private static BigDecimal ratio(long part, long whole) {
        if (whole <= 0) {
            return BigDecimal.ZERO.setScale(RATE_SCALE, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(part)
                .divide(BigDecimal.valueOf(whole), RATE_SCALE, RoundingMode.HALF_UP);
    }
}
