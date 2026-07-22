package com.umc.product.project.adapter.in.graphql.dto;

import com.umc.product.project.domain.enums.MatchingType;

public enum ProjectMatchingType {
    PLAN_DESIGN,
    PLAN_DEVELOPER;

    public static ProjectMatchingType from(MatchingType type) {
        return valueOf(type.name());
    }
}
