package com.umc.product.organization.adapter.in.web.dto.response.school;

import com.umc.product.organization.application.port.in.query.dto.school.SchoolListItemInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "학교 목록 항목")
public record SchoolListItemResponse(
    @Schema(description = "학교 ID", example = "1")
    Long schoolId,

    @Schema(description = "학교명", example = "서울대학교")
    String schoolName,

    @Schema(description = "학교 약칭", example = "서울대")
    String shortName,

    @Schema(description = "지부 ID", example = "1")
    Long chapterId,

    @Schema(description = "지부명", example = "서울")
    String chapterName,

    @Schema(description = "생성일시")
    Instant createdAt,

    @Schema(description = "활성 여부 (현재 기수 활동 중)", example = "true")
    boolean isActive,

    @Schema(description = "비고")
    String remark,

    @Schema(description = "학교 로고 이미지 URL")
    String logoImageUrl
) {

    public static SchoolListItemResponse of(SchoolListItemInfo summary) {
        return new SchoolListItemResponse(summary.schoolId(), summary.schoolName(), summary.shortName(),
            summary.chapterId(), summary.chapterName(), summary.createdAt(), summary.isActive(),
            summary.remark(), summary.logoImageUrl());
    }
}

