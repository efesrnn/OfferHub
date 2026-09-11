package com.offerhub.ai.service;

import com.offerhub.ai.dto.RecommendResponse;
import com.offerhub.ai.dto.SubscriberInsightResponse;
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
     * Abonenin profilini ve segmentini, o segmenti tetikleyen ham sinyalden turetilmis
     * okunabilir bir "reason" ile birlikte doner. Gercek Identity abonesi (AI'da veri
     * biriktirilmemis) icin buildFallbackProfile devreye giriyor - o durumda da reason,
     * o abone icin uretilen (hash'e bagli, deterministik) ham degerlerden hesaplaniyor,
     * yani ayni segmentteki iki abone hala birbirinden ayirt edilebiliyor.
     */
    public SubscriberInsightResponse getInsight(String subscriberId) {
        SubscriberProfile profile = subscriberProfileRepository.findById(subscriberId)
                .orElseGet(() -> buildFallbackProfile(subscriberId));

        String segment = scoringEngine.predictSegment(profile);
        String reason = reasonFor(segment, profile);

        return new SubscriberInsightResponse(
                subscriberId,
                segment,
                reason,
                profile.getTenureMonths(),
                round2(profile.getMonthlyDataUsageGb()),
                round2(profile.getMonthlySpendTry()),
                profile.getComplaintCount6m(),
                round2(profile.getUsageTrend()),
                profile.getPastAcceptedOffers(),
                profile.getPastDeclinedOffers(),
                profile.getCurrentTariff());
    }

    /**
     * Karar agaci segmenti tek bir etikete indiriyor; bu ayni etiketin arkasindaki en
     * belirgin ham sinyali secip okunabilir bir cumleye ceviriyor - amac, ayni segmentteki
     * iki aboneyi ("neden ikisi de PASIF ama farkli") birbirinden ayirt edebilmek.
     * Esikler generate_data.py'deki uretim araliklarina gore kalibre edildi, egitim
     * etiketindeki tam z-skor formulunun (populasyon ortalamasi gerektirir) basitlestirilmis
     * bir aciklama karsiligidir - segment karari hala modelden geliyor, bu sadece "neden".
     */
    private String reasonFor(String segment, SubscriberProfile p) {
        return switch (segment) {
            case "YENI_ABONE" -> "Yeni abone (%d aydir kayitli)".formatted(p.getTenureMonths());
            case "RISKLI_KAYIP" -> {
                if (p.getComplaintCount6m() >= 3) {
                    yield "Son 6 ayda %d sikayet".formatted(p.getComplaintCount6m());
                } else if (p.getUsageTrend() < -0.1) {
                    yield "Kullanimi hizla dusuyor (trend: %.2f)".formatted(p.getUsageTrend());
                } else if (p.getPastDeclinedOffers() > p.getPastAcceptedOffers()) {
                    yield "Gecmis tekliflerin cogunu reddetmis (%d ret / %d kabul)"
                            .formatted(p.getPastDeclinedOffers(), p.getPastAcceptedOffers());
                } else {
                    yield "Kayip riski sinyalleri var (sikayet + dusen kullanim)";
                }
            }
            case "YUKSEK_DEGER" -> {
                if (p.getMonthlySpendTry() > 400) {
                    yield "Yuksek aylik harcama (%.0f TL)".formatted(p.getMonthlySpendTry());
                } else if (p.getMonthlyDataUsageGb() > 20) {
                    yield "Yogun veri kullanicisi (%.1f GB/ay)".formatted(p.getMonthlyDataUsageGb());
                } else if (p.getPastAcceptedOffers() > p.getPastDeclinedOffers()) {
                    yield "Gecmiste tekliflere olumlu yanit vermis (%d kabul)"
                            .formatted(p.getPastAcceptedOffers());
                } else {
                    yield "Genel degeri yuksek (harcama + kullanim + sadakat birlesimi)";
                }
            }
            case "PASIF" -> {
                if (p.getMonthlyDataUsageGb() < 5) {
                    yield "Veri kullanimi cok dusuk (%.1f GB/ay)".formatted(p.getMonthlyDataUsageGb());
                } else if (p.getMonthlySpendTry() < 150) {
                    yield "Aylik harcama cok dusuk (%.0f TL)".formatted(p.getMonthlySpendTry());
                } else if (p.getPastAcceptedOffers() == 0 && p.getPastDeclinedOffers() == 0) {
                    yield "Hic kampanya gecmisi yok";
                } else {
                    yield "Genel kullanim/etkilesim dusuk";
                }
            }
            default -> "Belirlenemedi";
        };
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
