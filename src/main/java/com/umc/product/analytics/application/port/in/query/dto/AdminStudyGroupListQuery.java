package com.umc.product.analytics.application.port.in.query.dto;

public record AdminStudyGroupListQuery(
    Long requesterMemberId,
    Long gisuId,
    Long schoolId
) {

    public static AdminStudyGroupListQuery of(Long requesterMemberId, Long gisuId, Long schoolId) {
        return new AdminStudyGroupListQuery(requesterMemberId, gisuId, schoolId);
    }
}
