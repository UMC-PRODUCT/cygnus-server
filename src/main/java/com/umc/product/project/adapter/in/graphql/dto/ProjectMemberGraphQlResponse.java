package com.umc.product.project.adapter.in.graphql.dto;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;
import com.umc.product.project.application.port.in.query.dto.ProjectMemberInfo;
import com.umc.product.project.domain.enums.ProjectMemberStatus;

/**
 * ProjectMember 응답. {@code projectId}/{@code applicationId}/{@code memberId}는
 * 배치 로딩용 raw ID로 스키마에 노출하지 않는다.
 */
public record ProjectMemberGraphQlResponse(
    String projectMemberId,
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
            GlobalId.encode(GlobalIdTypes.PROJECT_MEMBER, info.projectMemberId()),
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
