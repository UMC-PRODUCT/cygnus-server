package com.umc.product.form.application.port.in.command.dto;

import java.util.List;
import lombok.Builder;

/**
 * 섹션 내 질문 순서 재배치 Command.
 * {@code orderedQuestionIds} 의 순서대로 각 질문의 orderNo가 1부터 재부여.
 */
@Builder
public record ReorderQuestionsCommand(
    Long sectionId,
    Long requesterMemberId,
    List<Long> orderedQuestionIds
) {
}
