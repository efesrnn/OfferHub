package com.offerhub.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RecommendResponse {
    private double score;
    private double conversionProbability;
    private String segment;
}
