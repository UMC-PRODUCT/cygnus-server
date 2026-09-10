package com.umc.product.analytics.application.port.in.query.dto;

import java.time.Instant;

public record AdminOperationsCommunityActivityQuery(
    Long requesterMemberId,
    Long gisuId,
    Instant from,
    Instant to,
    Granularity granularity
) {

    public static AdminOperationsCommunityActivityQuery of(
        Long requesterMemberId,
        Long gisuId,
        Instant from,
        Instant to,
        Granularity granularity
    ) {
        return new AdminOperationsCommunityActivityQuery(requesterMemberId, gisuId, from, to, granularity);
    }
}
