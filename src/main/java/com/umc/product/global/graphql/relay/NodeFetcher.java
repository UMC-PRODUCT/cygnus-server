package com.umc.product.global.graphql.relay;

import org.springframework.lang.Nullable;

/**
 * {@code node(id:)} 루트 필드에서 전역 ID의 타입별 재조회를 담당하는 SPI.
 * 각 도메인의 GraphQL 어댑터가 구현하며, 단건 조회와 동일한 권한 검증을 수행해야 한다.
 */
public interface NodeFetcher {

    String typeName();

    /**
     * 대상이 존재하지 않으면 null을 반환한다. 권한이 없으면 예외를 던진다.
     */
    @Nullable RelayNode fetchOrNull(long rawId);
}
