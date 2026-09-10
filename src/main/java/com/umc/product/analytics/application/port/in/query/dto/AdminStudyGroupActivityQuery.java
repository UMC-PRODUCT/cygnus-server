package com.umc.product.analytics.application.port.in.query.dto;

public record AdminStudyGroupActivityQuery(
    Long requesterMemberId,
    Long gisuId
) {

    public static AdminStudyGroupActivityQuery of(Long requesterMemberId, Long gisuId) {
        return new AdminStudyGroupActivityQuery(requesterMemberId, gisuId);
    }
}
