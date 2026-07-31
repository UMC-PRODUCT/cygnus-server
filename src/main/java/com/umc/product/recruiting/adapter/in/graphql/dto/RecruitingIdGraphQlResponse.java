package com.umc.product.recruiting.adapter.in.graphql.dto;

public record RecruitingIdGraphQlResponse(
    Long id
) {

    public static RecruitingIdGraphQlResponse from(Long id) {
        return new RecruitingIdGraphQlResponse(id);
    }
}
