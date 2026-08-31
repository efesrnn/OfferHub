package com.offerhub.campaign.security;

/** Mirrors Identity's Role enum - the gateway forwards the name as a header value. */
public enum Role {
    SUBSCRIBER,
    EXPERT,
    SUPERVISOR,
    ADMIN
}
