package com.offerhub.ai.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RecommendRequest {
    private String subscriberId;
    private String campaignType;
}
