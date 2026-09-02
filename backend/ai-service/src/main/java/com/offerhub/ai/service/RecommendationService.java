package com.offerhub.ai.service;

import com.offerhub.ai.dto.RecommendResponse;
import com.offerhub.ai.entity.SubscriberProfile;
import com.offerhub.ai.repository.SubscriberProfileRepository;
import com.offerhub.ai.scoring.ScoringEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Random;

@RequiredArgsConstructor
@Service
public class RecommendationService {

    private static final String[] TARIFFS = {"EKONOMIK", "STANDART", "PREMIUM"};

    private final SubscriberProfileRepository subscriberProfileRepository;
    private final ScoringEngine scoringEngine;
    private final AccuracyService accuracyService;

    public RecommendResponse recommend(String subscriberId, String campaignType) {
        SubscriberProfile profile = subscriberProfileRepository.findById(subscriberId)
                .orElseGet(() -> buildFallbackProfile(subscriberId));

        double score = scoringEngine.predictConversionScore(profile, campaignType);
        String segment = scoringEngine.predictSegment(profile);
        accuracyService.recordPrediction(segment);

        return new RecommendResponse(round2(score), round2(score), segment);
    }

    /**
     * Identity Service'te kayıtlı olup AI Service'in henüz kullanım verisi
     * biriktirmediği (gerçek/yeni) abonelikler için: subscriberId'den
     * deterministik bir sahte profil üretir. Aynı ID her zaman aynı profili
     * verir (rastgele değil), ama farklı ID'ler farklı profil üretir —
     * yani çıktı hâlâ girdiye bağlı değişir.
     */
    private SubscriberProfile buildFallbackProfile(String subscriberId) {
        long seed = subscriberId.hashCode();
        Random rnd = new Random(seed);

        SubscriberProfile p = new SubscriberProfile();
        p.setSubscriberId(subscriberId);
        p.setTenureMonths(1 + rnd.nextInt(24)); // yeni abone varsayımı
        p.setMonthlyDataUsageGb(2 + rnd.nextDouble() * 15);
        p.setMonthlyVoiceMinutes(rnd.nextInt(1500));
        p.setMonthlySpendTry(80 + rnd.nextDouble() * 250);
        p.setCurrentTariff(TARIFFS[rnd.nextInt(TARIFFS.length)]);
        p.setPastAcceptedOffers(0);
        p.setPastDeclinedOffers(0);
        p.setComplaintCount6m(rnd.nextInt(2));
        p.setUsageTrend(rnd.nextDouble() * 0.4 - 0.2);
        return p;
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
