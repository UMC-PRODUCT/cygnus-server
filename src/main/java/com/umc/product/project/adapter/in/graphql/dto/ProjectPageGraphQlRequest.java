package com.umc.product.project.adapter.in.graphql.dto;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public record ProjectPageGraphQlRequest(
    Integer page,
    Integer size,
    List<ProjectSort> sort
) {
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final List<ProjectSort> DEFAULT_SORT = List.of(ProjectSort.CREATED_AT_ASC, ProjectSort.NAME_ASC);

    public Pageable toPageable() {
        int pageNumber = page == null ? DEFAULT_PAGE : page;
        int pageSize = size == null ? DEFAULT_SIZE : size;
        if (pageNumber < 0) {
            throw new IllegalArgumentException("page must be greater than or equal to 0");
        }
        if (pageSize <= 0 || pageSize > MAX_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAX_SIZE);
        }

        List<ProjectSort> sortValues = (sort == null || sort.isEmpty()) ? DEFAULT_SORT : sort;
        return PageRequest.of(pageNumber, pageSize, Sort.by(sortValues.stream()
            .map(ProjectSort::toOrder)
            .toList()));
    }

    public enum ProjectSort {
        CREATED_AT_ASC("createdAt", Sort.Direction.ASC),
        CREATED_AT_DESC("createdAt", Sort.Direction.DESC),
        NAME_ASC("name", Sort.Direction.ASC),
        NAME_DESC("name", Sort.Direction.DESC);

        private final String property;
        private final Sort.Direction direction;

        ProjectSort(String property, Sort.Direction direction) {
            this.property = property;
            this.direction = direction;
        }

        private Sort.Order toOrder() {
            return new Sort.Order(direction, property);
        }
    }
}
