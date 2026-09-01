package com.offerhub.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

@Getter
@AllArgsConstructor
public class AccuracyResponse {
    private double overallAccuracy;
    private Map<String, Double> bySegment;
}
