package com.umc.product.analytics.application.port.in.query.dto;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.umc.product.analytics.domain.AnalyticsDomainException;
import com.umc.product.analytics.domain.AnalyticsErrorCode;

public record AdminOperationsAttendanceTagsQuery(
    Long requesterMemberId,
    Long gisuId,
    Instant from,
    Instant to
) {

    public static AdminOperationsAttendanceTagsQuery of(
        Long requesterMemberId,
        Long gisuId,
        Instant from,
        Instant to
    ) {
        Instant normalizedTo = to != null ? to : Instant.now();
        Instant normalizedFrom = from != null ? from : normalizedTo.minus(30, ChronoUnit.DAYS);
        if (!normalizedFrom.isBefore(normalizedTo)) {
            throw new AnalyticsDomainException(AnalyticsErrorCode.INVALID_PERIOD);
        }
        return new AdminOperationsAttendanceTagsQuery(requesterMemberId, gisuId, normalizedFrom, normalizedTo);
    }
}
