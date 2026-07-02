package com.umc.product.recruiting.adapter.in.web.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "생성된 리크루팅 리소스 ID 응답")
public record RecruitingIdResponse(
    @Schema(description = "생성된 리소스 ID", example = "1")
    Long id
) {

    public static RecruitingIdResponse from(Long id) {
        return new RecruitingIdResponse(id);
    }
}
