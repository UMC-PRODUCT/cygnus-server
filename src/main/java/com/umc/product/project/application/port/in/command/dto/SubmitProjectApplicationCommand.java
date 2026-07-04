package com.umc.product.project.application.port.in.command.dto;

import lombok.Builder;

/**
 * 챌린저 지원서 최종 제출 Command (APPLY-003).
 * <p>
 * path의 applicationId로 본인의 DRAFT 지원서를 명시적으로 식별한다.
 * DRAFT -> SUBMITTED 전이. 필수 답변 누락 검증은 Form {@code ManageFormResponseUseCase.submitDraft}가 담당.
 */
@Builder
public record SubmitProjectApplicationCommand(
    Long projectId,
    Long applicationId,
    Long requesterMemberId
) {
}
