package com.umc.product.analytics.adapter.in.web.dto.request;

import java.time.Instant;

import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsAttendanceChaptersQuery;

public record AdminOperationsAttendanceChaptersRequest(
    Long gisuId,
    Instant from,
    Instant to
) {

    public AdminOperationsAttendanceChaptersQuery toQuery(Long requesterMemberId) {
        return AdminOperationsAttendanceChaptersQuery.of(requesterMemberId, gisuId, from, to);
    }
}
