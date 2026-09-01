package com.offerhub.ai.service;

import com.offerhub.ai.dto.AssignExpertResponse;
import com.offerhub.ai.entity.ExpertSnapshot;
import com.offerhub.ai.repository.ExpertSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class ExpertAssignmentService {

    private static final double WEIGHT_SPECIALTY_MATCH = 0.5;
    private static final double WEIGHT_CAPACITY = 0.3;
    private static final double WEIGHT_PERFORMANCE = 0.2;

    private final ExpertSnapshotRepository expertSnapshotRepository;

    public AssignExpertResponse assign(String caseId, String segment) {
        List<ExpertSnapshot> experts = expertSnapshotRepository.findAll();

        ExpertSnapshot bestExpert = null;
        double bestScore = -1;

        for (ExpertSnapshot expert : experts) {
            double capacityRatio = 1.0 - ((double) expert.getActiveCaseCount() / ExpertSnapshot.MAX_CAPACITY);
            if (capacityRatio <= 0) {
                continue; // kapasitesi dolu, atanamaz
            }
            double specialtyMatch = expert.getSpecialties().contains(segment) ? 1.0 : 0.0;
            double score = specialtyMatch * WEIGHT_SPECIALTY_MATCH
                    + capacityRatio * WEIGHT_CAPACITY
                    + expert.getPerformanceScore() * WEIGHT_PERFORMANCE;

            if (score > bestScore) {
                bestScore = score;
                bestExpert = expert;
            }
        }

        if (bestExpert == null) {
            return new AssignExpertResponse(null, null, true);
        }

        // Demo amaçlı: seçilen uzmanın aktif vaka sayısını artır (gerçek sistemde
        // bu, Campaign Service'in case-assigned event'iyle güncellenmeli)
        bestExpert.setActiveCaseCount(bestExpert.getActiveCaseCount() + 1);
        expertSnapshotRepository.save(bestExpert);

        return new AssignExpertResponse(bestExpert.getExpertId(), round2(bestScore), false);
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
