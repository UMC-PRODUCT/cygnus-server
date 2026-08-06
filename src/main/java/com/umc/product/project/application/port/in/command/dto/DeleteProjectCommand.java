package com.umc.product.project.application.port.in.command.dto;

import java.util.Objects;

import lombok.Builder;

/**
 * 프로젝트 hard delete Command.
 * <p>
 * DRAFT / PENDING_REVIEW 상태의 프로젝트와 연관 자식(ProjectMember, ProjectPartQuota,
 * ProjectApplicationForm + Policy + 연결된 form Form)을 cascade 삭제합니다.
 * IN_PROGRESS 이상 상태는 {@code AbortProjectUseCase} 로 상태 전이만 가능합니다.
 */
@Builder
public record DeleteProjectCommand(
    Long projectId,
    Long requesterMemberId
) {
    public DeleteProjectCommand {
        Objects.requireNonNull(projectId, "projectId must not be null");
        Objects.requireNonNull(requesterMemberId, "requesterMemberId must not be null");
    }
}
