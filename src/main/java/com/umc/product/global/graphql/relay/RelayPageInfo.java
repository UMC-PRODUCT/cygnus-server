package com.umc.product.global.graphql.relay;

import org.springframework.lang.Nullable;

/**
 * Relay Connection 스펙의 PageInfo 타입.
 */
public record RelayPageInfo(
    boolean hasNextPage,
    boolean hasPreviousPage,
    @Nullable String startCursor,
    @Nullable String endCursor
) {
}
