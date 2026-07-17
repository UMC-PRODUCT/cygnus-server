package com.umc.product.project.adapter.in.graphql.dto;

import com.umc.product.member.application.port.in.query.dto.MemberInfo;

public record ProjectMemberSummaryGraphQlResponse(
    Long memberId,
    String nickname,
    String name,
    String schoolName
) {
    public static ProjectMemberSummaryGraphQlResponse from(MemberInfo info) {
        return new ProjectMemberSummaryGraphQlResponse(
            info.id(),
            info.nickname(),
            info.name(),
            info.schoolName()
        );
    }
}
