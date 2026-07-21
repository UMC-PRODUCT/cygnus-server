package com.umc.product.recruiting.adapter.in.graphql.dto;

import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;

public record RecruitingStatusCountGraphQlResponse(
    RecruitingApplicationStatus status,
    Long count
) {
}
