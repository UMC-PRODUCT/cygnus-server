package com.umc.product.analytics.application.port.in.query.dto;

public record AdminGisuPointsQuery(
    Long requesterMemberId,
    Long gisuId
) {
    public static AdminGisuPointsQuery of(Long requesterMemberId, Long gisuId) {
        return new AdminGisuPointsQuery(requesterMemberId, gisuId);
    }
}
