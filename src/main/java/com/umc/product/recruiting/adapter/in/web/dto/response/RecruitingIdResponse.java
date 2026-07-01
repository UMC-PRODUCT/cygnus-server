package com.umc.product.recruiting.adapter.in.web.dto.response;

public record RecruitingIdResponse(
    Long id
) {

    public static RecruitingIdResponse from(Long id) {
        return new RecruitingIdResponse(id);
    }
}
