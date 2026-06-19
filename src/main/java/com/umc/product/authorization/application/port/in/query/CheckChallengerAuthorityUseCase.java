package com.umc.product.authorization.application.port.in.query;

import com.umc.product.common.domain.enums.ChallengerRoleType;

/**
 * ChallengerRole 기반 권한 판정 전용 UseCase입니다.
 */
public interface CheckChallengerAuthorityUseCase {

    boolean isSuperAdmin(Long memberId);

    boolean hasRoleTypeInGisu(Long memberId, Long gisuId, ChallengerRoleType role);

    boolean hasAnyRoleTypeInGisu(Long memberId, Long gisuId, ChallengerRoleType... roles);

    boolean hasAllRoleTypeInGisu(Long memberId, Long gisuId, ChallengerRoleType... roles);

    boolean isCentralCoreInGisu(Long memberId, Long gisuId);

    boolean isCentralMemberInGisu(Long memberId, Long gisuId);

    boolean isSchoolCoreInGisu(Long memberId, Long gisuId, Long schoolId);

    boolean isSchoolAdminInGisu(Long memberId, Long gisuId, Long schoolId);

    boolean isChapterPresidentInGisu(Long memberId, Long gisuId, Long chapterId);
}
