package com.umc.product.curriculum.application.port.in.command.dto.workbook.mission;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

/**
 * 미션 제출물 철회 커맨드 (챌린저 전용)
 *
 * @param missionSubmissionId 철회 대상 미션 제출물 ID
 * @param requesterMemberId   요청 챌린저의 멤버 ID (본인 제출물 여부 검증용)
 */
@Builder
public record WithdrawMissionSubmissionCommand(
    @NotNull(message = "미션 제출물 ID는 필수입니다")
    Long missionSubmissionId,

    @NotNull(message = "요청자 멤버 ID는 필수입니다")
    Long requesterMemberId
) {
}
