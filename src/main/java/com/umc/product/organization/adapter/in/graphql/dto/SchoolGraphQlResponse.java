package com.umc.product.organization.adapter.in.graphql.dto;

import java.time.Instant;
import java.util.List;

import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;
import com.umc.product.global.graphql.relay.RelayNode;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolDetailInfo;
import com.umc.product.organization.domain.enums.SchoolLinkType;

/**
 * 스키마 {@code School}(Node)과 {@code GisuSchool} 타입 응답.
 * rawSchoolId/rawChapterId는 스키마에 노출하지 않으며, id/schoolId/chapterId는 전역 ID로 인코딩해 노출한다.
 */
public record SchoolGraphQlResponse(
    Long rawSchoolId,
    Long rawChapterId,
    String chapterName,
    String schoolName,
    String remark,
    String logoImageUrl,
    List<SchoolLinkGraphQlResponse> links,
    boolean active,
    String createdAt,
    String updatedAt
) implements RelayNode {

    public static SchoolGraphQlResponse from(SchoolDetailInfo info) {
        List<SchoolLinkGraphQlResponse> links = info.links() == null
            ? List.of()
            : info.links().stream()
                .map(SchoolLinkGraphQlResponse::from)
                .toList();

        return new SchoolGraphQlResponse(
            info.schoolId(),
            info.chapterId(),
            info.chapterName(),
            info.schoolName(),
            info.remark(),
            info.logoImageUrl(),
            links,
            info.isActive(),
            format(info.createdAt()),
            format(info.updatedAt())
        );
    }

    @Override
    public String id() {
        return GlobalId.encode(GlobalIdTypes.SCHOOL, rawSchoolId);
    }

    /**
     * {@code GisuSchool.schoolId} 필드용 — School 전역 ID와 동일한 값이다.
     */
    public String schoolId() {
        return id();
    }

    public String chapterId() {
        return rawChapterId == null ? null : GlobalId.encode(GlobalIdTypes.CHAPTER, rawChapterId);
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
