package com.umc.product.project.application.port.in.command;

import com.umc.product.project.application.port.in.command.dto.CompleteProjectsCommand;

/**
 * 프로젝트 완료(complete) UseCase.
 * <p>
 * IN_PROGRESS 상태 프로젝트들을 COMPLETED 로 전이합니다. ACTIVE ProjectMember 는 COMPLETED,
 * 진행 중(DRAFT/SUBMITTED) ProjectApplication 은 CANCELLED 로 일괄 동기화합니다.
 * <p>
 * 배치 처리이며, 대상 중 하나라도 MANAGE 권한이 없거나 IN_PROGRESS 가 아니면 전체 롤백합니다.
 */
public interface CompleteProjectsUseCase {

    /**
     * 프로젝트들을 완료 처리합니다.
     */
    void complete(CompleteProjectsCommand command);
}
