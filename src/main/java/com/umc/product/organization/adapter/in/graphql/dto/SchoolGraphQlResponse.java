package com.umc.product.organization.adapter.in.graphql.dto;

import java.time.Instant;
import java.util.List;

import com.umc.product.organization.application.port.in.query.dto.school.SchoolDetailInfo;
import com.umc.product.organization.domain.enums.SchoolLinkType;

public record SchoolGraphQlResponse(
    Long id,
    String name,
    String remark,
    String logoImageUrl,
    List<SchoolLinkGraphQlResponse> links,
    boolean active,
    Instant createdAt,
    Instant updatedAt
) {

    public static SchoolGraphQlResponse from(SchoolDetailInfo info) {
        List<SchoolLinkGraphQlResponse> links = info.links() == null
            ? List.of()
            : info.links().stream()
                .map(SchoolLinkGraphQlResponse::from)
                .toList();

        return new SchoolGraphQlResponse(
            info.schoolId(),
            info.schoolName(),
            info.remark(),
            info.logoImageUrl(),
            links,
            info.isActive(),
            info.createdAt(),
            info.updatedAt()
        );
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
