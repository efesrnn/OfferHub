package com.offerhub.campaign.service;

import com.offerhub.campaign.dto.ConversionTrendPoint;
import com.offerhub.campaign.dto.DashboardResponse;
import com.offerhub.campaign.dto.ExpertPerformance;
import com.offerhub.campaign.entity.CaseStatus;
import com.offerhub.campaign.entity.Offer;
import com.offerhub.campaign.entity.OfferStatus;
import com.offerhub.campaign.entity.OptimizationCase;
import com.offerhub.campaign.entity.Segment;
import com.offerhub.campaign.repository.CampaignRepository;
import com.offerhub.campaign.repository.OfferRepository;
import com.offerhub.campaign.repository.OptimizationCaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    /** Two weeks of trend: long enough to see a direction on a mobile chart. */
    private static final int TREND_DAYS = 14;

    /** Case document 6.3: an expert carries ten active cases at most. */
    private static final int EXPERT_CAPACITY = 10;

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
                caseRepository.countByStatus(CaseStatus.YENI),
                ratio(campaignRepository.countClassifiedCorrectly(), campaignRepository.countClassified()),
                conversionTrend(),
                expertPerformance());
    }

    /**
     * Conversion per day for the last two weeks. Grouped in Java rather than in SQL: the
     * answered set is small, and a date_trunc query would tie this to one database dialect
     * for no measurable gain.
     */
    private List<ConversionTrendPoint> conversionTrend() {
        Instant since = Instant.now().minus(TREND_DAYS, ChronoUnit.DAYS);

        Map<LocalDate, List<Offer>> perDay = offerRepository.findAnsweredSince(since).stream()
                .collect(Collectors.groupingBy(offer ->
                        offer.getRespondedAt().atZone(ZoneOffset.UTC).toLocalDate()));

        return perDay.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    long answered = entry.getValue().size();
                    long accepted = entry.getValue().stream()
                            .filter(offer -> offer.getStatus() == OfferStatus.ACCEPTED)
                            .count();
                    return new ConversionTrendPoint(entry.getKey(), ratio(accepted, answered), answered);
                })
                .toList();
    }

    /**
     * Case document 8.1: completed cases, average conversion improvement and average time
     * per expert, plus what they are carrying now.
     *
     * Only experts who have finished something appear - a row of zeroes for someone who
     * never had a case says nothing, and this service does not know the staff list anyway.
     */
    private List<ExpertPerformance> expertPerformance() {
        Map<UUID, Long> activePerExpert = caseRepository.findOpenWithExpert().stream()
                .collect(Collectors.groupingBy(OptimizationCase::getAssignedExpertId,
                        Collectors.counting()));

        return caseRepository.findCompletedWithExpert().stream()
                .collect(Collectors.groupingBy(OptimizationCase::getAssignedExpertId))
                .entrySet().stream()
                .map(entry -> toPerformance(entry.getKey(), entry.getValue(),
                        activePerExpert.getOrDefault(entry.getKey(), 0L)))
                .sorted(Comparator.comparingLong(ExpertPerformance::completedCases).reversed())
                .toList();
    }

    private static ExpertPerformance toPerformance(UUID expertId, List<OptimizationCase> completed,
                                                   long activeCases) {
        BigDecimal averageLift = average(completed.stream()
                .map(OptimizationCase::getConversionLift)
                .filter(java.util.Objects::nonNull)
                .toList());

        BigDecimal averageHours = average(completed.stream()
                .map(DashboardService::hoursToComplete)
                .toList());

        return new ExpertPerformance(expertId, completed.size(), averageLift, averageHours,
                Math.min(activeCases, EXPERT_CAPACITY));
    }

    /** Hours from the case being opened to it being completed, one decimal. */
    private static BigDecimal hoursToComplete(OptimizationCase optimizationCase) {
        Duration took = Duration.between(optimizationCase.getCreatedAt(), optimizationCase.getCompletedAt());
        return BigDecimal.valueOf(took.toMinutes())
                .divide(BigDecimal.valueOf(60), 1, RoundingMode.HALF_UP);
    }

    /** Null rather than zero when there is nothing to average - none measured is not "no lift". */
    private static BigDecimal average(List<BigDecimal> values) {
        if (values.isEmpty()) {
            return null;
        }
        return values.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(values.size()), 3, RoundingMode.HALF_UP);
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
