package com.offerhub.campaign.dto;

import com.offerhub.campaign.entity.CampaignType;
import com.offerhub.campaign.entity.Priority;
import com.offerhub.campaign.entity.Segment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Correcting what AI decided. The role matrix treats segment and type as one permission,
 * so they travel together; priority is a supervisor's call and is checked separately.
 *
 * All three are optional and only the fields present are applied - a request that changes
 * the segment should not have to restate the type it is not touching.
 *
 * @param reason required, not decoration: this correction feeds AI's accuracy metric, and
 *               a correction nobody can explain later is not evidence of anything
 */
public record ClassificationRequest(

        Segment segment,

        CampaignType type,

        Priority priority,

        @NotBlank
        @Size(max = 500)
        @Pattern(regexp = "[^<>]*", message = "must not contain < or >")
        String reason
) {

    /** A request that changes nothing is a mistake worth reporting rather than a no-op. */
    public boolean isEmpty() {
        return segment == null && type == null && priority == null;
    }
}
