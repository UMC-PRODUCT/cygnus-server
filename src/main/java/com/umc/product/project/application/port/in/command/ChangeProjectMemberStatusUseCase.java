package com.umc.product.project.application.port.in.command;

import com.umc.product.project.application.port.in.command.dto.ChangeProjectMemberStatusCommand;

/**
 * 프로젝트 멤버 상태 변경 UseCase (PROJECT-006, soft delete).
 * <p>
 * 멤버 행을 보존한 채 {@code status} 만 변경(COMPLETED/WITHDRAWN/DISMISSED 등)하고 사유·주체를 기록한다.
 * 행 자체를 지워 재등록을 허용해야 하면 {@link RemoveProjectMemberUseCase}(PROJECT-005, hard delete) 를 사용한다.
 */
public interface ChangeProjectMemberStatusUseCase {

    void changeStatus(ChangeProjectMemberStatusCommand command);
}
