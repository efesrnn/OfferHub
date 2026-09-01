package com.offerhub.campaign.dto;

import com.offerhub.campaign.entity.CaseStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * @param optimizationNote required only when moving to TAMAMLANDI, optional elsewhere -
 *                         a rule the state machine cannot see, so the service checks it
 */
public record StatusChangeRequest(

        @NotNull CaseStatus targetStatus,

        @Size(max = 1000)
        @Pattern(regexp = "[^<>]*", message = "must not contain < or >")
        String optimizationNote
) {
}
