package com.offerhub.campaign.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OfferRateRequest(@NotNull @Min(1) @Max(5) Integer stars) {
}
