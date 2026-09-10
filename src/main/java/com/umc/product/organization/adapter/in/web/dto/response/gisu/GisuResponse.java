package com.umc.product.organization.adapter.in.web.dto.response.gisu;

import java.time.Instant;

import com.umc.product.common.domain.enums.GisuLearningType;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "기수 정보")
public record GisuResponse(
    @Schema(description = "기수 ID", example = "1")
    Long gisuId,
    @Schema(description = "기수 번호", example = "8")
    Long generation, // TODO: gisu로 마이그레이션 후 제거할 것
    Long gisu,
    @Schema(description = "시작일", example = "2024-03-01T00:00:00Z")
    Instant startAt,
    @Schema(description = "종료일", example = "2024-08-31T23:59:59Z")
    Instant endAt,
    @Schema(description = "활성 여부", example = "true")
    boolean isActive,
    GisuLearningType learningType
) {
    public static GisuResponse from(GisuInfo info) {
        return new GisuResponse(info.gisuId(), info.generation(), info.generation(), info.startAt(), info.endAt(),
            info.isActive(), info.learningType());
    }
}
