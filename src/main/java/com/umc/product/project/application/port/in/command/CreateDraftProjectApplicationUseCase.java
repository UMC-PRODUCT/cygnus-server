package com.umc.product.project.application.port.in.command;

import com.umc.product.project.application.port.in.command.dto.CreateDraftProjectApplicationCommand;
import com.umc.product.project.application.port.in.query.dto.ProjectApplicationInfo;

/**
 * 챌린저 지원서 Draft 생성 UseCase (APPLY-001).
 * <p>
 * 인증된 챌린저가 특정 프로젝트의 지원서를 Draft 상태로 신규 생성한다.
 * 동일 (projectId, applicantMemberId) 조합에 이미 DRAFT 지원서가 있으면 기존 application 정보를 그대로 반환한다.
 * Form 의 {@code ManageFormResponseUseCase.createDraft}를 함께 호출하여 빈 응답지(FormResponse, status=DRAFT)를 매핑한다.
 */
public interface CreateDraftProjectApplicationUseCase {
    ProjectApplicationInfo create(CreateDraftProjectApplicationCommand command);
}
