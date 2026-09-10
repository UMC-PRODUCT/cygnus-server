package com.umc.product.analytics.adapter.in.web.dto.request;

import com.umc.product.analytics.application.port.in.query.dto.AdminGisuSummaryQuery;

public record AdminGisuSummaryRequest(Long gisuId) {
    public AdminGisuSummaryQuery toQuery(Long requesterMemberId) {
        return AdminGisuSummaryQuery.of(requesterMemberId, gisuId);
    }
}
