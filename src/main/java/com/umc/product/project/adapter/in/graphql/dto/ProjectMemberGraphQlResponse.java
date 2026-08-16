package com.umc.product.project.adapter.in.graphql.dto;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.project.application.port.in.query.dto.ProjectMemberInfo;
import com.umc.product.project.domain.enums.ProjectMemberStatus;

public record ProjectMemberGraphQlResponse(
    Long projectMemberId,
    Long projectId,
    Long applicationId,
    Long memberId,
    ChallengerPart part,
    boolean leader,
    String description,
    String decidedAt,
    ProjectMemberStatus status
) {
    public static ProjectMemberGraphQlResponse from(ProjectMemberInfo info) {
        return new ProjectMemberGraphQlResponse(
            info.projectMemberId(),
            info.projectId(),
            info.applicationId(),
            info.memberId(),
            info.part(),
            info.isLeader(),
            info.description(),
            info.decidedAt() == null ? null : info.decidedAt().toString(),
            info.status()
        );
    }
}
