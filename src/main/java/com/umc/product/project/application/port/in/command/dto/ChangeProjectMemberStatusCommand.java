package com.umc.product.project.application.port.in.command.dto;

import java.util.Objects;

import com.umc.product.project.domain.enums.ProjectMemberStatus;

import lombok.Builder;

/**
 * 프로젝트 멤버 상태 변경 Command (PROJECT-006, soft delete).
 * <p>
 * 행을 보존한 채 {@code status} 로 변경하고 {@code reason} 을 감사 이력으로 기록한다.
 * 동일 멤버 재등록이 필요하면 hard delete API({@code PROJECT-005}) 를 사용한다.
 * <p>
 * 정책:
 * <ul>
 *   <li>메인 PM 상태 변경 거부 — 소유권 양도 API({@code PROJECT-104}) 로 유도</li>
 *   <li>COMPLETED/ABORTED 프로젝트: 거부 — 종료된 프로젝트의 이력 고정</li>
 * </ul>
 */
@Builder
public record ChangeProjectMemberStatusCommand(
    Long projectId,
    Long memberId,
    ProjectMemberStatus status,
    String reason,
    Long requesterMemberId
) {
    public ChangeProjectMemberStatusCommand {
        Objects.requireNonNull(projectId, "projectId must not be null");
        Objects.requireNonNull(memberId, "memberId must not be null");
        Objects.requireNonNull(status, "status must not be null");
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("reason must not be null or blank");
        }
        Objects.requireNonNull(requesterMemberId, "requesterMemberId must not be null");
    }
}
