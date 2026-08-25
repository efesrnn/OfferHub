package com.offerhub.campaign.service;

import com.offerhub.campaign.entity.CampaignCounter;
import com.offerhub.campaign.repository.CampaignCounterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.time.Year;

/** Produces the readable campaign number, e.g. CMP-2026-000123. */
@Service
@RequiredArgsConstructor
public class CampaignNumberGenerator {

    private static final String FORMAT = "CMP-%d-%06d";

    private final CampaignCounterRepository counterRepository;

    /**
     * Joins the caller's transaction on purpose: the row lock is held until the campaign
     * insert commits, so a rolled back creation does not burn a number.
     */
    @Transactional
    public String next() {
        int year = Year.now(ZoneOffset.UTC).getValue();

        // Only the very first campaign of a year takes the insert branch.
        CampaignCounter counter = counterRepository.findByYear(year)
                .orElseGet(() -> counterRepository.saveAndFlush(new CampaignCounter(year, 0)));

        counter.setLastValue(counter.getLastValue() + 1);
        return FORMAT.formatted(year, counter.getLastValue());
    }
}