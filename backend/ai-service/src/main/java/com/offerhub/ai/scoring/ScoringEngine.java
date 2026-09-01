package com.offerhub.ai.scoring;

import com.offerhub.ai.entity.SubscriberProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class ScoringEngine {

    private final ModelLoader modelLoader;

    public double predictConversionScore(SubscriberProfile profile, String campaignType) {
        ModelWeights.RecommendationModel model = modelLoader.getWeights().recommendationModel;
        Map<String, Double> features = baseFeatures(profile);
        oneHotInto(features, "currentTariff", profile.getCurrentTariff(),
                List.of("EKONOMIK", "STANDART", "PREMIUM"));
        oneHotInto(features, "campaignType", campaignType,
                List.of("EK_PAKET", "TARIFE_YUKSELTME", "CIHAZ_FIRSATI", "SADAKAT"));

        double z = model.bias;
        for (int i = 0; i < model.featureOrder.size(); i++) {
            String name = model.featureOrder.get(i);
            double raw = features.getOrDefault(name, 0.0);
            double standardized = (raw - model.mean.get(i)) / model.std.get(i);
            z += standardized * model.weights.get(i);
        }
        return sigmoid(z);
    }

    public String predictSegment(SubscriberProfile profile) {
        ModelWeights.SegmentModel model = modelLoader.getWeights().segmentModel;
        Map<String, Double> features = baseFeatures(profile);
        oneHotInto(features, "currentTariff", profile.getCurrentTariff(),
                List.of("EKONOMIK", "STANDART", "PREMIUM"));

        double[] vector = new double[model.featureOrder.size()];
        for (int i = 0; i < model.featureOrder.size(); i++) {
            vector[i] = features.getOrDefault(model.featureOrder.get(i), 0.0);
        }

        int nodeIndex = 0;
        List<ModelWeights.TreeNode> nodes = model.nodes;
        while (true) {
            ModelWeights.TreeNode node = nodes.get(nodeIndex);
            if (node.isLeaf) {
                return node.predictedClass;
            }
            double value = vector[node.featureIndex];
            nodeIndex = (value <= node.threshold) ? node.left : node.right;
        }
    }

    private Map<String, Double> baseFeatures(SubscriberProfile p) {
        Map<String, Double> f = new HashMap<>();
        f.put("tenureMonths", (double) p.getTenureMonths());
        f.put("monthlyDataUsageGb", p.getMonthlyDataUsageGb());
        f.put("monthlyVoiceMinutes", p.getMonthlyVoiceMinutes());
        f.put("monthlySpendTry", p.getMonthlySpendTry());
        f.put("pastAcceptedOffers", (double) p.getPastAcceptedOffers());
        f.put("pastDeclinedOffers", (double) p.getPastDeclinedOffers());
        f.put("complaintCount6m", (double) p.getComplaintCount6m());
        f.put("usageTrend", p.getUsageTrend());
        return f;
    }

    private void oneHotInto(Map<String, Double> features, String prefix, String value, List<String> categories) {
        for (String cat : categories) {
            features.put(prefix + "_" + cat, cat.equals(value) ? 1.0 : 0.0);
        }
    }

    private double sigmoid(double z) {
        return 1.0 / (1.0 + Math.exp(-z));
    }
}