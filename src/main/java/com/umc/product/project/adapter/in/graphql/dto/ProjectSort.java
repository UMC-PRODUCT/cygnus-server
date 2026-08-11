package com.umc.product.project.adapter.in.graphql.dto;

import java.util.List;

import org.springframework.data.domain.Sort;

/**
 * projects 정렬 옵션. orderBy 인자가 비어 있으면 CREATED_AT_ASC, NAME_ASC 순서를 기본으로 사용한다.
 */
public enum ProjectSort {
    CREATED_AT_ASC("createdAt", Sort.Direction.ASC),
    CREATED_AT_DESC("createdAt", Sort.Direction.DESC),
    NAME_ASC("name", Sort.Direction.ASC),
    NAME_DESC("name", Sort.Direction.DESC);

    private static final List<ProjectSort> DEFAULT_ORDER = List.of(CREATED_AT_ASC, NAME_ASC);

    private final String property;
    private final Sort.Direction direction;

    ProjectSort(String property, Sort.Direction direction) {
        this.property = property;
        this.direction = direction;
    }

    public static Sort toSort(List<ProjectSort> orderBy) {
        List<ProjectSort> effective = (orderBy == null || orderBy.isEmpty()) ? DEFAULT_ORDER : orderBy;
        return Sort.by(effective.stream()
            .map(ProjectSort::toOrder)
            .toList());
    }

    private Sort.Order toOrder() {
        return new Sort.Order(direction, property);
    }
}
