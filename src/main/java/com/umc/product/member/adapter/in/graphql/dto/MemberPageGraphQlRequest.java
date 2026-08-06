package com.umc.product.member.adapter.in.graphql.dto;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

public record MemberPageGraphQlRequest(
    Integer page,
    Integer size
) {
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final long MAX_OFFSET = 10_000L;

    public Pageable toPageable() {
        int pageNumber = page == null ? DEFAULT_PAGE : page;
        int pageSize = size == null ? DEFAULT_SIZE : size;
        if (pageNumber < 0) {
            throw new IllegalArgumentException("page must be greater than or equal to 0");
        }
        if (pageSize <= 0 || pageSize > MAX_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAX_SIZE);
        }
        long offset = (long) pageNumber * pageSize;
        if (offset > MAX_OFFSET) {
            throw new IllegalArgumentException("page offset must be less than or equal to " + MAX_OFFSET);
        }
        return PageRequest.of(pageNumber, pageSize);
    }
}
