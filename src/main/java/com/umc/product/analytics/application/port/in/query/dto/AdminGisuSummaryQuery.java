package com.umc.product.analytics.application.port.in.query.dto;

public record AdminGisuSummaryQuery(
    Long requesterMemberId,
    Long gisuId
) {
    public static AdminGisuSummaryQuery of(Long requesterMemberId, Long gisuId) {
        return new AdminGisuSummaryQuery(requesterMemberId, gisuId);
    }
}
