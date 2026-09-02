package com.offerhub.campaign.dto;

import com.offerhub.campaign.entity.OfferStatus;
import jakarta.validation.constraints.NotNull;

/**
 * @param response ACCEPTED or DECLINED PENDING is rejected - it is the starting state,
 *                 not an answer a subscriber can give
 */
public record OfferRespondRequest(@NotNull OfferStatus response) {
}
