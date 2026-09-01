package com.offerhub.ai.service;

import com.offerhub.ai.dto.AccuracyResponse;
import com.offerhub.ai.entity.MisclassificationLog;
import com.offerhub.ai.entity.SegmentPredictionCounter;
import com.offerhub.ai.repository.MisclassificationLogRepository;
import com.offerhub.ai.repository.SegmentPredictionCounterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class AccuracyService {

    private static final List<String> SEGMENTS = List.of(
            "YUKSEK_DEGER", "RISKLI_KAYIP", "YENI_ABONE", "PASIF");

    private final SegmentPredictionCounterRepository counterRepository;
    private final MisclassificationLogRepository misclassificationLogRepository;

    public void recordPrediction(String segment) {
        SegmentPredictionCounter counter = counterRepository.findById(segment)
                .orElseGet(() -> new SegmentPredictionCounter(segment, 0));
        counter.setTotalCount(counter.getTotalCount() + 1);
        counterRepository.save(counter);
    }

    public void recordMisclassification(String campaignNo, String originalSegment, String correctedSegment) {
        misclassificationLogRepository.save(
                new MisclassificationLog(campaignNo, originalSegment, correctedSegment));
    }

    public AccuracyResponse getAccuracy() {
        List<MisclassificationLog> allMisclassifications = misclassificationLogRepository.findAll();

        long totalPredictions = 0;
        long totalMisclassified = 0;
        Map<String, Double> bySegment = new LinkedHashMap<>();

        for (String segment : SEGMENTS) {
            long segmentTotal = counterRepository.findById(segment)
                    .map(SegmentPredictionCounter::getTotalCount)
                    .orElse(0L);
            long segmentWrong = allMisclassifications.stream()
                    .filter(m -> segment.equals(m.getOriginalSegment()))
                    .count();

            totalPredictions += segmentTotal;
            totalMisclassified += segmentWrong;

            if (segmentTotal > 0) {
                bySegment.put(segment, round2(1.0 - ((double) segmentWrong / segmentTotal)));
            }
        }

        double overallAccuracy = totalPredictions > 0
                ? round2(1.0 - ((double) totalMisclassified / totalPredictions))
                : 1.0;

        return new AccuracyResponse(overallAccuracy, bySegment);
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
