package com.umc.product.project.adapter.in.graphql.dto;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.project.application.port.in.query.dto.ProjectPartQuotaInfo;
import com.umc.product.project.domain.enums.PartQuotaStatus;

public record ProjectPartQuotaGraphQlResponse(
    ChallengerPart part,
    long quota,
    long currentCount,
    long remainingCount,
    PartQuotaStatus status
) {
    public static ProjectPartQuotaGraphQlResponse from(ProjectPartQuotaInfo info) {
        return new ProjectPartQuotaGraphQlResponse(
            info.part(),
            info.quota(),
            info.currentCount(),
            Math.max(info.quota() - info.currentCount(), 0),
            info.status()
        );
    }
}
