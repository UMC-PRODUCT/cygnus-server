package com.umc.product.organization.adapter.in.web.v2.dto.response;

import java.time.Instant;
import java.util.List;

import com.umc.product.common.domain.enums.GisuLearningType;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuOrganizationInfo;
import com.umc.product.organization.domain.enums.SchoolLinkType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "기수 조직 조회 v2 응답")
public record GisuOrganizationV2Response(
    @Schema(description = "기수 목록")
    List<GisuItem> gisus
) {

    public static GisuOrganizationV2Response from(List<GisuOrganizationInfo> infos) {
        return new GisuOrganizationV2Response(
            infos.stream()
                .map(GisuItem::from)
                .toList()
        );
    }

    @Schema(description = "기수 정보")
    public record GisuItem(
        @Schema(description = "기수 ID", example = "1")
        Long gisuId,

        @Schema(description = "기수 번호", example = "10")
        Long generation,

        @Schema(description = "시작일", example = "2026-03-01T00:00:00Z")
        Instant startAt,

        @Schema(description = "종료일", example = "2026-08-31T23:59:59Z")
        Instant endAt,

        @Schema(description = "활성 여부", example = "true")
        boolean isActive,

        @Schema(description = "기수 내 지부 목록")
        List<ChapterItem> chapters,

        @Schema(description = "기수 내 학교 목록")
        List<SchoolItem> schools,

        GisuLearningType learningType
    ) {

        public static GisuItem from(GisuOrganizationInfo info) {
            return new GisuItem(
                info.gisuId(),
                info.generation(),
                info.startAt(),
                info.endAt(),
                info.isActive(),
                info.chapters().stream().map(ChapterItem::from).toList(),
                info.schools().stream().map(SchoolItem::from).toList(),
                info.learningType()
            );
        }
    }

    @Schema(description = "지부 정보")
    public record ChapterItem(
        @Schema(description = "지부 ID", example = "1")
        Long chapterId,

        @Schema(description = "지부명", example = "서울지부")
        String chapterName,

        @Schema(description = "지부 내 학교 목록")
        List<ChapterSchoolItem> schools
    ) {

        public static ChapterItem from(GisuOrganizationInfo.ChapterOrganizationInfo info) {
            return new ChapterItem(
                info.chapterId(),
                info.chapterName(),
                info.schools().stream().map(ChapterSchoolItem::from).toList()
            );
        }
    }

    @Schema(description = "지부 내 학교 정보")
    public record ChapterSchoolItem(
        @Schema(description = "학교 ID", example = "1")
        Long schoolId,

        @Schema(description = "학교명", example = "중앙대학교")
        String schoolName
    ) {

        public static ChapterSchoolItem from(GisuOrganizationInfo.ChapterSchoolInfo info) {
            return new ChapterSchoolItem(info.schoolId(), info.schoolName());
        }
    }

    @Schema(description = "기수 내 학교 상세 정보")
    public record SchoolItem(
        @Schema(description = "지부 ID", example = "1")
        Long chapterId,

        @Schema(description = "지부명", example = "서울지부")
        String chapterName,

        @Schema(description = "학교 ID", example = "1")
        Long schoolId,

        @Schema(description = "학교명", example = "중앙대학교")
        String schoolName,

        @Schema(description = "학교 약칭", example = "중앙대")
        String shortName,

        @Schema(description = "비고")
        String remark,

        @Schema(description = "학교 로고 이미지 URL")
        String logoImageUrl,

        @Schema(description = "학교 링크 목록")
        List<SchoolLinkItem> links,

        @Schema(description = "활성 기수 배정 여부", example = "true")
        boolean isActive,

        @Schema(description = "생성일")
        Instant createdAt,

        @Schema(description = "수정일")
        Instant updatedAt
    ) {

        public static SchoolItem from(GisuOrganizationInfo.SchoolOrganizationInfo info) {
            return new SchoolItem(
                info.chapterId(),
                info.chapterName(),
                info.schoolId(),
                info.schoolName(),
                info.shortName(),
                info.remark(),
                info.logoImageUrl(),
                info.links().stream().map(SchoolLinkItem::from).toList(),
                info.isActive(),
                info.createdAt(),
                info.updatedAt()
            );
        }
    }

    @Schema(description = "학교 링크 정보")
    public record SchoolLinkItem(
        @Schema(description = "링크 제목")
        String title,

        @Schema(description = "링크 타입")
        SchoolLinkType type,

        @Schema(description = "링크 URL")
        String url
    ) {

        public static SchoolLinkItem from(GisuOrganizationInfo.SchoolLinkInfo info) {
            return new SchoolLinkItem(info.title(), info.type(), info.url());
        }
    }
}
