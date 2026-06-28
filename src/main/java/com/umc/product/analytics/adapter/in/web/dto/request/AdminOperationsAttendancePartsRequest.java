package com.umc.product.analytics.adapter.in.web.dto.request;

import java.time.Instant;

import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsAttendancePartsQuery;

public record AdminOperationsAttendancePartsRequest(
    Long gisuId,
    Instant from,
    Instant to
) {

    public AdminOperationsAttendancePartsQuery toQuery(Long requesterMemberId) {
        return AdminOperationsAttendancePartsQuery.of(requesterMemberId, gisuId, from, to);
    }
}
