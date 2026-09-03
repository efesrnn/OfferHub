package com.offerhub.campaign.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** The client sends "rating"; our own endpoint calls the same thing "stars". */
public record RateOfferRequest(@NotNull @Min(1) @Max(5) Integer rating) {
}
