package com.offerhub.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StaffCreateResponse {
    private String staffId;
    private boolean tempPasswordSent;
    private String tempPassword;
}
