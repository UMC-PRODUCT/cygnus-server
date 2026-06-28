package com.umc.product.analytics.adapter.in.web.dto.request;

import java.time.Instant;

import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsAttendanceTagsQuery;

public record AdminOperationsAttendanceTagsRequest(
    Long gisuId,
    Instant from,
    Instant to
) {

    public AdminOperationsAttendanceTagsQuery toQuery(Long requesterMemberId) {
        return AdminOperationsAttendanceTagsQuery.of(requesterMemberId, gisuId, from, to);
    }
}
