package com.umc.product.project.application.port.in.command;

import com.umc.product.project.application.port.in.command.dto.RemoveProjectMemberCommand;

/**
 * 프로젝트 멤버 hard delete UseCase (PROJECT-005).
 * <p>
 * 멤버 행을 물리적으로 삭제해 동일 멤버의 재등록을 가능하게 한다. 상태 이력 보존이 필요하면
 * {@link ChangeProjectMemberStatusUseCase}(PROJECT-006) 를 사용한다.
 */
public interface RemoveProjectMemberUseCase {

    void remove(RemoveProjectMemberCommand command);
}
