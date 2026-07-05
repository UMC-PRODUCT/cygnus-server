package com.umc.product.curriculum.application.port.in.command.dto.workbook;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

/**
 * 챌린저 워크북 삭제 커맨드 (운영진 전용)
 *
 * @param challengerWorkbookId 삭제 대상 챌린저 워크북 ID
 * @param requestedMemberId    요청 운영진의 멤버 ID (감사 추적용)
 */
@Builder
public record DeleteChallengerWorkbookCommand(
    @NotNull(message = "챌린저 워크북 ID는 필수입니다") Long challengerWorkbookId,

    @NotNull(message = "요청자 멤버 ID는 필수입니다") Long requestedMemberId
) {
}
