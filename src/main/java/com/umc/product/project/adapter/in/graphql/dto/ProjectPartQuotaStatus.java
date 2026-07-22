package com.umc.product.project.adapter.in.graphql.dto;

import com.umc.product.project.domain.enums.PartQuotaStatus;

public enum ProjectPartQuotaStatus {
    RECRUITING,
    COMPLETED;

    public static ProjectPartQuotaStatus from(PartQuotaStatus status) {
        return valueOf(status.name());
    }

    public PartQuotaStatus toDomain() {
        return PartQuotaStatus.valueOf(name());
    }
}
