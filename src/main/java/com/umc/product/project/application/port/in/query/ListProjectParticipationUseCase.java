package com.umc.product.project.application.port.in.query;

import java.util.Collection;
import java.util.Set;

/**
 * 프로젝트 참여자 조회 UseCase.
 *
 * <p>메인 PM과 활성 {@code ProjectMember}를 모두 프로젝트 참여자로 취급한다.
 */
public interface ListProjectParticipationUseCase {

    Set<Long> listParticipatingProjectIds(Collection<Long> projectIds, Long memberId);
}
