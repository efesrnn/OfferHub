package com.offerhub.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AssignExpertResponse {
    private String expertId;
    private Double matchScore;
    private boolean queued;
}
