package com.offerhub.ai.config;

import com.offerhub.ai.entity.ExpertSnapshot;
import com.offerhub.ai.entity.SubscriberProfile;
import com.offerhub.ai.repository.ExpertSnapshotRepository;
import com.offerhub.ai.repository.SubscriberProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class DataSeeder implements CommandLineRunner {

    private final SubscriberProfileRepository subscriberProfileRepository;
    private final ExpertSnapshotRepository expertSnapshotRepository;

    @Override
    public void run(String... args) throws Exception {
        seedSubscriberProfiles();
        seedExpertSnapshots();
    }

    private void seedSubscriberProfiles() throws Exception {
        if (subscriberProfileRepository.count() > 0) {
            return;
        }
        List<SubscriberProfile> profiles = new ArrayList<>();
        ClassPathResource resource = new ClassPathResource("ai-model/subscriber_profiles.csv");
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String header = reader.readLine(); // başlık satırını atla
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] c = line.split(",", -1);
                // subscriberId,firstName,tenureMonths,monthlyDataUsageGb,monthlyVoiceMinutes,
                // monthlySpendTry,currentTariff,pastAcceptedOffers,pastDeclinedOffers,
                // complaintCount6m,usageTrend,segment
                SubscriberProfile p = new SubscriberProfile();
                p.setSubscriberId(c[0]);
                p.setTenureMonths(Integer.parseInt(c[2]));
                p.setMonthlyDataUsageGb(Double.parseDouble(c[3]));
                p.setMonthlyVoiceMinutes(Double.parseDouble(c[4]));
                p.setMonthlySpendTry(Double.parseDouble(c[5]));
                p.setCurrentTariff(c[6]);
                p.setPastAcceptedOffers(Integer.parseInt(c[7]));
                p.setPastDeclinedOffers(Integer.parseInt(c[8]));
                p.setComplaintCount6m(Integer.parseInt(c[9]));
                p.setUsageTrend(Double.parseDouble(c[10]));
                profiles.add(p);
            }
        }
        subscriberProfileRepository.saveAll(profiles);
        log.info("AI Service: {} sentetik abone profili yüklendi.", profiles.size());
    }

    private void seedExpertSnapshots() {
        if (expertSnapshotRepository.count() > 0) {
            return;
        }
        List<ExpertSnapshot> experts = List.of(
                expert("EXP-001", List.of("RISKLI_KAYIP"), 3, 0.82),
                expert("EXP-002", List.of("YUKSEK_DEGER"), 6, 0.74),
                expert("EXP-003", List.of("YENI_ABONE", "PASIF"), 2, 0.65),
                expert("EXP-004", List.of("RISKLI_KAYIP", "YUKSEK_DEGER"), 9, 0.90),
                expert("EXP-005", List.of("PASIF"), 1, 0.55)
        );
        expertSnapshotRepository.saveAll(experts);
        log.info("AI Service: {} demo uzman kaydı yüklendi.", experts.size());
    }

    private ExpertSnapshot expert(String id, List<String> specialties, int activeCases, double performance) {
        ExpertSnapshot e = new ExpertSnapshot();
        e.setExpertId(id);
        e.setSpecialties(specialties);
        e.setActiveCaseCount(activeCases);
        e.setPerformanceScore(performance);
        return e;
    }
}