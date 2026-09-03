package com.offerhub.campaign.service;

import com.offerhub.campaign.client.AiClient;
import com.offerhub.campaign.client.AiRecommendation;
import com.offerhub.campaign.entity.CampaignType;
import com.offerhub.campaign.entity.Priority;
import com.offerhub.campaign.entity.Segment;
import com.offerhub.campaign.entity.SubscriberProjection;
import com.offerhub.campaign.repository.SubscriberProjectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Turns "score one subscriber" into "score a campaign".
 *
 * AI answers per subscriber, but a campaign is aimed at a segment, so somebody has to
 * bridge the two. That is done by sampling: a campaign targeting YUKSEK_DEGER is scored
 * against a sample of the subscribers we hold for that segment, and the results are
 * averaged. Scoring all 220 would multiply campaign creation by 220 HTTP calls for a
 * number that barely moves after the first twenty.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CampaignAiAdvisor {

    /** Large enough for a stable average, small enough to keep creation quick. */
    private static final int SAMPLE_SIZE = 20;

    /** One failure is enough: AiClient then refuses to try again for a cooldown period. */
    private static final int FAILURES_BEFORE_GIVING_UP = 1;

    /**
     * Case document 5.3: AI assigns priority from conversion potential. The worse a
     * campaign is expected to perform, the more urgently it needs an expert - so the
     * thresholds run the opposite way to the probability.
     */
    private static final BigDecimal CRITICAL_BELOW = new BigDecimal("0.30");
    private static final BigDecimal HIGH_BELOW = new BigDecimal("0.50");
    private static final BigDecimal MEDIUM_BELOW = new BigDecimal("0.70");

    private final AiClient aiClient;
    private final SubscriberProjectionRepository projectionRepository;

    /**
     * NOT_SUPPORTED: the sampling reads run outside the caller's transaction, so a slow or
     * failing AI call never holds a database transaction open while it waits on a socket.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED, readOnly = true)
    public CampaignScoring scoreFor(Segment targetSegment, CampaignType campaignType) {
        List<SubscriberProjection> sample = projectionRepository
                .findBySegment(targetSegment, PageRequest.of(0, SAMPLE_SIZE));

        if (sample.isEmpty()) {
            log.info("No subscribers known for segment {}, campaign stays unscored", targetSegment);
            return CampaignScoring.unavailable();
        }

        List<AiRecommendation> answers = ask(sample, campaignType);

        // Every call failed, or none of the sampled subscribers came from the seed set and
        // so has a code AI would recognise. Either way there is nothing to average.
        if (answers.isEmpty()) {
            log.warn("AI returned nothing for segment {}, using the fallback path", targetSegment);
            return CampaignScoring.unavailable();
        }

        BigDecimal conversionProbability = average(answers.stream()
                .map(AiRecommendation::conversionProbability).toList());
        BigDecimal recommendationScore = average(answers.stream()
                .map(AiRecommendation::score).toList());
        Segment segment = majoritySegment(answers, targetSegment);

        log.info("AI scored a {} campaign from {}/{} answers: segment={} conversion={} score={}",
                campaignType, answers.size(), sample.size(), segment,
                conversionProbability, recommendationScore);

        return new CampaignScoring(segment, conversionProbability, recommendationScore,
                priorityFor(conversionProbability, segment));
    }

    /**
     * Walks the sample, and stops early once it is clear nobody is answering.
     *
     * Without that check an unreachable AI costs one timeout per sampled subscriber, so a
     * hung service would turn a three second timeout into a minute of blocked campaign
     * creation. Two failures in a row is not a coincidence - it is the service being down,
     * and the remaining calls would only confirm it more slowly.
     */
    private List<AiRecommendation> ask(List<SubscriberProjection> sample, CampaignType campaignType) {
        List<AiRecommendation> answers = new ArrayList<>();
        int consecutiveFailures = 0;

        for (SubscriberProjection subscriber : sample) {
            String reference = subscriber.getExternalRef();
            if (reference == null || reference.isBlank()) {
                continue;
            }

            Optional<AiRecommendation> answer = aiClient.recommend(reference, campaignType);
            if (answer.isPresent()) {
                answers.add(answer.get());
                consecutiveFailures = 0;
                continue;
            }

            if (++consecutiveFailures >= FAILURES_BEFORE_GIVING_UP) {
                log.warn("Abandoning the sample after {} consecutive AI failures", consecutiveFailures);
                break;
            }
        }
        return answers;
    }

    /**
     * The segment most of the sample fell into. AI classifies each subscriber on their own
     * behaviour, so a sample drawn from one segment can still answer with a few others;
     * the majority is the campaign's classification, and ties keep what was targeted.
     */
    private static Segment majoritySegment(List<AiRecommendation> answers, Segment fallback) {
        Map<String, Long> counts = answers.stream()
                .map(AiRecommendation::segment)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.groupingBy(name -> name, Collectors.counting()));

        return counts.entrySet().stream()
                .max(Map.Entry.comparingByValue(Comparator.naturalOrder()))
                .map(Map.Entry::getKey)
                .map(name -> parse(name, fallback))
                .orElse(fallback);
    }

    /** An unknown segment name means AI's vocabulary drifted from ours - do not crash on it. */
    private static Segment parse(String name, Segment fallback) {
        try {
            return Segment.valueOf(name);
        } catch (IllegalArgumentException ex) {
            log.warn("AI returned an unknown segment '{}', keeping {}", name, fallback);
            return fallback;
        }
    }

    /**
     * Case document 5.3, both rules in one place: priority follows the conversion estimate,
     * and a churn risk campaign is never below YUKSEK whatever the estimate says.
     */
    private static Priority priorityFor(BigDecimal conversionProbability, Segment segment) {
        Priority priority = fromProbability(conversionProbability);

        if (segment == Segment.RISKLI_KAYIP && priority.compareTo(Priority.YUKSEK) < 0) {
            return Priority.YUKSEK;
        }
        return priority;
    }

    private static Priority fromProbability(BigDecimal probability) {
        if (probability == null) {
            return Priority.ORTA;
        }
        if (probability.compareTo(CRITICAL_BELOW) < 0) {
            return Priority.KRITIK;
        }
        if (probability.compareTo(HIGH_BELOW) < 0) {
            return Priority.YUKSEK;
        }
        if (probability.compareTo(MEDIUM_BELOW) < 0) {
            return Priority.ORTA;
        }
        return Priority.DUSUK;
    }

    /** Three decimals, matching the column the result is stored in. */
    private static BigDecimal average(List<BigDecimal> values) {
        List<BigDecimal> present = values.stream().filter(java.util.Objects::nonNull).toList();
        if (present.isEmpty()) {
            return null;
        }
        return present.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(present.size()), 3, RoundingMode.HALF_UP);
    }
}
