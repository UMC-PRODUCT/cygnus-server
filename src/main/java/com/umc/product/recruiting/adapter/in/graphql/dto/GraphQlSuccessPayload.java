package com.umc.product.recruiting.adapter.in.graphql.dto;

/**
 * Boolean 결과만 갖는 뮤테이션 payload 공용 레코드. 스키마의 모든 {@code success: Boolean!} payload 타입에 매핑된다.
 */
public record GraphQlSuccessPayload(boolean success) {

    public static GraphQlSuccessPayload ok() {
        return new GraphQlSuccessPayload(true);
    }
}
