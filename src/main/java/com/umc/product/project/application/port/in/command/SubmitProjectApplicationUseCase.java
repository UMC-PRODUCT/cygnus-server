package com.umc.product.project.application.port.in.command;

import com.umc.product.project.application.port.in.command.dto.SubmitProjectApplicationCommand;
import com.umc.product.project.application.port.in.query.dto.ProjectApplicationInfo;

/**
 * 챌린저 지원서 최종 제출 UseCase (APPLY-003).
 * <p>
 * DRAFT -> SUBMITTED 전이. 본인만 호출 가능.
 * Form 의 {@code ManageFormResponseUseCase.submitDraft}가 필수 답변 누락 검증을 수행하고 FormResponse를 SUBMITTED로 전이시킨다.
 */
public interface SubmitProjectApplicationUseCase {
    ProjectApplicationInfo submit(SubmitProjectApplicationCommand command);
}
