package com.offerhub.ai.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MisclassificationRequest {
    private String campaignNo;
    private String originalSegment;
    private String correctedSegment;
}
