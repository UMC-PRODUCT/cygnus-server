package com.umc.product.analytics.adapter.in.web.dto.request;

import com.umc.product.analytics.application.port.in.query.dto.AdminGisuPointsQuery;

public record AdminGisuPointsRequest(Long gisuId) {
    public AdminGisuPointsQuery toQuery(Long requesterMemberId) {
        return AdminGisuPointsQuery.of(requesterMemberId, gisuId);
    }
}
