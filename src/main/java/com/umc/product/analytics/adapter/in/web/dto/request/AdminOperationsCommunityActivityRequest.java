package com.umc.product.analytics.adapter.in.web.dto.request;

import java.time.Instant;

import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsCommunityActivityQuery;
import com.umc.product.analytics.application.port.in.query.dto.Granularity;

public record AdminOperationsCommunityActivityRequest(
    Long gisuId,
    Instant from,
    Instant to,
    Granularity granularity
) {

    public AdminOperationsCommunityActivityQuery toQuery(Long requesterMemberId) {
        Granularity resolvedGranularity = granularity != null ? granularity : Granularity.WEEKLY;
        return AdminOperationsCommunityActivityQuery.of(requesterMemberId, gisuId, from, to, resolvedGranularity);
    }
}
