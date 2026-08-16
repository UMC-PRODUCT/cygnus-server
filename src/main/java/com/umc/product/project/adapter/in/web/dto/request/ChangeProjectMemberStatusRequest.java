package com.umc.product.project.adapter.in.web.dto.request;

import com.umc.product.project.application.port.in.command.dto.ChangeProjectMemberStatusCommand;
import com.umc.product.project.domain.enums.ProjectMemberStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * PROJECT-006 요청 (멤버 상태 변경, soft delete).
 * <p>
 * 변경할 {@code status} 와 사유를 함께 전달한다. {@code reason} 은 행의
 * {@code status_change_reason} 컬럼에 영속화되는 정식 변경 이력이므로 필수.
 */
public record ChangeProjectMemberStatusRequest(
    @NotNull(message = "status 는 필수입니다") ProjectMemberStatus status,

    @NotBlank(message = "reason 은 필수입니다") @Size(max = 255) String reason
) {
    public ChangeProjectMemberStatusCommand toCommand(Long projectId, Long memberId, Long requesterMemberId) {
        return ChangeProjectMemberStatusCommand.builder()
            .projectId(projectId)
            .memberId(memberId)
            .status(status)
            .reason(reason)
            .requesterMemberId(requesterMemberId)
            .build();
    }
}
