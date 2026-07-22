package com.umc.product.global.graphql.dto;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public record PageGraphQlRequest(
    Integer page,
    Integer size
) {
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    public static PageGraphQlRequest defaultIfNull(PageGraphQlRequest request) {
        return request == null ? new PageGraphQlRequest(null, null) : request;
    }

    public Pageable toPageable() {
        return toPageable(Sort.unsorted());
    }

    public Pageable toPageable(Sort sort) {
        int pageNumber = pageNumber();
        int pageSize = pageSize();
        return PageRequest.of(pageNumber, pageSize, sort);
    }

    public Pageable toPageableWithMaxOffset(long maxOffset) {
        int pageNumber = pageNumber();
        int pageSize = pageSize();
        long offset = (long) pageNumber * pageSize;
        if (offset > maxOffset) {
            throw new IllegalArgumentException("page offset must be less than or equal to " + maxOffset);
        }
        return PageRequest.of(pageNumber, pageSize);
    }

    private int pageNumber() {
        int pageNumber = page == null ? DEFAULT_PAGE : page;
        if (pageNumber < 0) {
            throw new IllegalArgumentException("page must be greater than or equal to 0");
        }
        return pageNumber;
    }

    private int pageSize() {
        int pageSize = size == null ? DEFAULT_SIZE : size;
        if (pageSize <= 0 || pageSize > MAX_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAX_SIZE);
        }
        return pageSize;
    }
}
