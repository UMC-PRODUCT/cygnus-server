package com.umc.product.member.adapter.in.graphql.dto;

import java.time.Instant;
import java.util.List;

import com.umc.product.organization.application.port.in.query.dto.school.SchoolDetailInfo;
import com.umc.product.organization.domain.enums.SchoolLinkType;

public record MemberSchoolGraphQlResponse(
    Long chapterId,
    String chapterName,
    Long schoolId,
    String schoolName,
    String remark,
    String logoImageUrl,
    List<SchoolLinkGraphQlResponse> links,
    boolean active,
    String createdAt,
    String updatedAt
) {

    public static MemberSchoolGraphQlResponse from(SchoolDetailInfo info) {
        List<SchoolLinkGraphQlResponse> links = info.links() == null
            ? List.of()
            : info.links().stream()
                .map(SchoolLinkGraphQlResponse::from)
                .toList();

        return new MemberSchoolGraphQlResponse(
            info.chapterId(),
            info.chapterName(),
            info.schoolId(),
            info.schoolName(),
            info.remark(),
            info.logoImageUrl(),
            links,
            info.isActive(),
            format(info.createdAt()),
            format(info.updatedAt())
        );
    }

    private static String format(Instant instant) {
        return instant == null ? null : instant.toString();
    }

    public record SchoolLinkGraphQlResponse(
        String title,
        SchoolLinkType type,
        String url
    ) {

        public static SchoolLinkGraphQlResponse from(SchoolDetailInfo.SchoolLinkItem info) {
            return new SchoolLinkGraphQlResponse(info.title(), info.type(), info.url());
        }
    }
}
