package com.offerhub.campaign.dto;

import com.offerhub.campaign.entity.Segment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SegmentOverrideRequest(

        @NotNull Segment segment,

        @NotBlank
        @Size(max = 500)
        @Pattern(regexp = "[^<>]*", message = "must not contain < or >")
        String reason
) {
}
