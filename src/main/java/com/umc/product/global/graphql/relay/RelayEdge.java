package com.umc.product.global.graphql.relay;

/**
 * Relay Connection 스펙의 Edge 타입.
 */
public record RelayEdge<T>(String cursor, T node) {
}
