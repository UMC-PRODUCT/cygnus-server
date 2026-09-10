package com.umc.product.organization.adapter.in.web.dto.response.gisu;

import com.umc.product.common.domain.enums.GisuLearningType;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "활성화된 기수 응답")
public record ActiveGisuResponse(
    @Schema(description = "기수 ID", example = "1")
    Long gisuId,
    @Schema(description = "기수 번호", example = "8")
    Long generation, // TODO: gisu로 마이그레이션 후 제거할 것
    Long gisu,
    GisuLearningType learningType
) {
    public static ActiveGisuResponse from(GisuInfo info) {
        return new ActiveGisuResponse(info.gisuId(), info.generation(), info.generation(), info.learningType());
    }
}
