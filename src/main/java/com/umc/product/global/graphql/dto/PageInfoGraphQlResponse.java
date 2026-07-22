package com.umc.product.global.graphql.dto;

import org.springframework.data.domain.Page;

public record PageInfoGraphQlResponse(
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean hasNext
) {
    public static PageInfoGraphQlResponse from(Page<?> page) {
        return new PageInfoGraphQlResponse(
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.hasNext()
        );
    }
}
