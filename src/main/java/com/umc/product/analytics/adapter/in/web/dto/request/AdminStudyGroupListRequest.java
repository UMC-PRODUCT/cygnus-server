package com.umc.product.analytics.adapter.in.web.dto.request;

import com.umc.product.analytics.application.port.in.query.dto.AdminStudyGroupListQuery;

public record AdminStudyGroupListRequest(
    Long gisuId,
    Long schoolId
) {

    public AdminStudyGroupListQuery toQuery(Long requesterMemberId) {
        return AdminStudyGroupListQuery.of(requesterMemberId, gisuId, schoolId);
    }
}
