package com.umc.product.analytics.adapter.in.web.dto.request;

import com.umc.product.analytics.application.port.in.query.dto.AdminStudyGroupActivityQuery;

public record AdminStudyGroupActivityRequest(
    Long gisuId
) {

    public AdminStudyGroupActivityQuery toQuery(Long requesterMemberId) {
        return AdminStudyGroupActivityQuery.of(requesterMemberId, gisuId);
    }
}
