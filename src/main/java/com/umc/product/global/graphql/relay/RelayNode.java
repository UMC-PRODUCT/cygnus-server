package com.umc.product.global.graphql.relay;

/**
 * 스키마에서 {@code Node} 인터페이스를 구현하는 GraphQL 응답 DTO의 마커 인터페이스.
 * id는 {@link GlobalId}로 인코딩된 전역 ID여야 하며, Node 타입 결정에 사용된다.
 */
public interface RelayNode {

    String id();
}
