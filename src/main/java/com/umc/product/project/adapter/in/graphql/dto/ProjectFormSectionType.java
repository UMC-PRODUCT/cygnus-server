package com.umc.product.project.adapter.in.graphql.dto;

import com.umc.product.project.domain.enums.FormSectionType;

public enum ProjectFormSectionType {
    COMMON,
    PART;

    public static ProjectFormSectionType from(FormSectionType type) {
        return valueOf(type.name());
    }
}
