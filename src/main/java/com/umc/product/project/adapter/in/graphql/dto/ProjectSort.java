package com.umc.product.project.adapter.in.graphql.dto;

import java.util.List;

import org.springframework.data.domain.Sort;

public enum ProjectSort {
    CREATED_AT_ASC("createdAt", Sort.Direction.ASC),
    CREATED_AT_DESC("createdAt", Sort.Direction.DESC),
    NAME_ASC("name", Sort.Direction.ASC),
    NAME_DESC("name", Sort.Direction.DESC);

    private static final List<ProjectSort> DEFAULT_SORT = List.of(CREATED_AT_ASC, NAME_ASC);

    private final String property;
    private final Sort.Direction direction;

    ProjectSort(String property, Sort.Direction direction) {
        this.property = property;
        this.direction = direction;
    }

    public static Sort toSpringSort(List<ProjectSort> values) {
        List<ProjectSort> effectiveValues = values == null || values.isEmpty() ? DEFAULT_SORT : values;
        return Sort.by(effectiveValues.stream().map(ProjectSort::toOrder).toList());
    }

    private Sort.Order toOrder() {
        return new Sort.Order(direction, property);
    }
}
