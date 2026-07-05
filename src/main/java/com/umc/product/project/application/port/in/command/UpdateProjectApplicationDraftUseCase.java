package com.umc.product.project.application.port.in.command;

import com.umc.product.project.application.port.in.command.dto.UpdateProjectApplicationDraftCommand;
import com.umc.product.project.application.port.in.query.dto.ProjectApplicationInfo;

/**
 * 챌린저 지원서 임시저장 UseCase (APPLY-002).
 * <p>
 * PUT 시멘틱 — 본문이 곧 새 전체 답변 상태가 된다. 본인만 호출 가능.
 * status 는 DRAFT(임시저장)일 때만 호출 가능하며, SUBMITTED 이후의 답변 수정은 별도 정책으로 처리한다.
 * Form의 {@code ManageFormResponseUseCase.updateDraft}에 위임한다.
 */
public interface UpdateProjectApplicationDraftUseCase {
    ProjectApplicationInfo update(UpdateProjectApplicationDraftCommand command);
}
