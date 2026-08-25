package com.offerhub.campaign.dto;

import com.offerhub.campaign.entity.CampaignType;
import com.offerhub.campaign.entity.Segment;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CreateCampaignRequest(

        @NotBlank
        @Size(max = 200)
        @Pattern(regexp = "[^<>]*", message = "must not contain < or >")
        String title,

        @NotNull CampaignType type,

        @NotNull Segment targetSegment,

        @NotNull @Min(0) @Max(100) Integer discountRate,

        @NotNull @Future Instant validUntil
) {
}