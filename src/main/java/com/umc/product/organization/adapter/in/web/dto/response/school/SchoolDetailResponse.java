package com.umc.product.organization.adapter.in.web.dto.response.school;

import java.time.Instant;
import java.util.List;

import com.umc.product.organization.application.port.in.query.dto.school.SchoolDetailInfo;
import com.umc.product.organization.domain.enums.SchoolLinkType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "학교 상세 정보")
public record SchoolDetailResponse(
    @Schema(description = "지부 ID", example = "1")
    Long chapterId,

    @Schema(description = "지부명", example = "서울")
    String chapterName,

    @Schema(description = "학교명", example = "서울대학교")
    String schoolName,

    @Schema(description = "학교 약칭", example = "서울대")
    String shortName,

    @Schema(description = "학교 ID", example = "1")
    Long schoolId,

    @Schema(description = "비고", example = "관악캠퍼스")
    String remark,

    @Schema(description = "로고 이미지 URL")
    String logoImageUrl,

    @Schema(description = "학교 링크 목록")
    List<SchoolLinkItem> links,

    @Schema(description = "활성 기수 배정 여부", example = "true")
    boolean isActive,

    @Schema(description = "생성일", example = "2024-03-01T00:00:00Z")
    Instant createdAt,

    @Schema(description = "수정일", example = "2024-03-15T00:00:00Z")
    Instant updatedAt
) {
    public static SchoolDetailResponse of(SchoolDetailInfo info) {
        List<SchoolLinkItem> linkItems = info.links() != null
            ? info.links().stream()
              .map(link -> new SchoolLinkItem(link.title(), link.type(), link.url()))
              .toList()
            : List.of();

        return new SchoolDetailResponse(
            info.chapterId(),
            info.chapterName(),
            info.schoolName(),
            info.shortName(),
            info.schoolId(),
            info.remark(),
            info.logoImageUrl(),
            linkItems,
            info.isActive(),
            info.createdAt(),
            info.updatedAt()
        );
    }

    public record SchoolLinkItem(
        String title,
        SchoolLinkType type,
        String url
    ) {
    }
}
