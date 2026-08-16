package com.umc.product.project.application.port.in.command.dto;

import java.util.Objects;

import lombok.Builder;

/**
 * 프로젝트 멤버 hard delete Command (PROJECT-005).
 * <p>
 * 행을 DB 에서 완전히 삭제하여 동일 멤버의 재등록을 가능하게 합니다. 히스토리 보존이 필요한 경우
 * 상태 변경(soft delete) API({@code PROJECT-006}) 를 사용합니다.
 * <p>
 * 정책:
 * <ul>
 *   <li>메인 PM 제거 거부 — 소유권 양도 API({@code PROJECT-104}) 로 유도</li>
 *   <li>DRAFT/PENDING_REVIEW/IN_PROGRESS: hard delete</li>
 *   <li>COMPLETED/ABORTED: 거부 — 종료된 프로젝트의 참여 이력 보존</li>
 * </ul>
 * {@code reason} 은 행과 함께 삭제되므로 DB 에는 남지 않으며, 삭제 직전 감사 로그로만 기록됩니다.
 */
@Builder
public record RemoveProjectMemberCommand(
    Long projectId,
    Long memberId,
    String reason,
    Long requesterMemberId
) {
    public RemoveProjectMemberCommand {
        Objects.requireNonNull(projectId, "projectId must not be null");
        Objects.requireNonNull(memberId, "memberId must not be null");
        Objects.requireNonNull(requesterMemberId, "requesterMemberId must not be null");
    }
}
