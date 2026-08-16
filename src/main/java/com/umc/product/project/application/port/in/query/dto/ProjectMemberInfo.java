package com.umc.product.project.application.port.in.query.dto;

import java.time.Instant;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.project.domain.ProjectMember;
import com.umc.product.project.domain.enums.ProjectMemberStatus;

import lombok.Builder;

/**
 * 프로젝트 멤버 정보를 담는 Info DTO.
 * <p>
 * Controller는 이 목록을 받아 Member 도메인의 {@code MemberInfo}를 추가 조회하여
 * Web 레이어의 {@code MemberBrief}/{@code ProjectMemberItem}으로 조립합니다.
 */
@Builder
public record ProjectMemberInfo(
    Long projectMemberId,
    Long projectId,
    Long applicationId,
    Long projectGisuId,
    String projectName,
    Long memberId,
    ChallengerPart part,
    boolean isLeader,
    String description,
    Instant decidedAt,
    ProjectMemberStatus status
) {
    public static ProjectMemberInfo from(ProjectMember entity) {
        return ProjectMemberInfo.builder()
            .projectMemberId(entity.getId())
            .projectId(entity.getProject().getId())
            .applicationId(entity.getApplication() == null ? null : entity.getApplication().getId())
            .projectGisuId(entity.getProject().getGisuId())
            .projectName(entity.getProject().getName())
            .memberId(entity.getMemberId())
            .part(entity.getPart())
            .isLeader(entity.isLeader())
            .description(entity.getDescription())
            .decidedAt(entity.getDecidedAt())
            .status(entity.getStatus())
            .build();
    }
}
