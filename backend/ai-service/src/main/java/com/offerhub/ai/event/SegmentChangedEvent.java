package com.offerhub.ai.event;

/** Our own copy of the segment.changed payload; fields we do not use are simply absent. */
public record SegmentChangedEvent(
        String campaignNo,
        String originalSegment,
        String correctedSegment
) {
}
