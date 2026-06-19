package com.umc.product.authorization.application.port.in.query;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.umc.product.authorization.application.port.in.query.dto.ChallengerRoleBasicInfo;
import com.umc.product.authorization.application.port.in.query.dto.ChallengerRoleInfo;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;

/**
 * 사용자의 역할 정보를 조회하는 UseCase
 * <p>
 * Notice, Curriculum 등 다른 도메인에서 사용자의 역할을 확인할 때 사용합니다.
 */
public interface GetChallengerRoleUseCase extends ListChallengerRoleUseCase, CheckChallengerAuthorityUseCase {

    default List<ChallengerRoleInfo> findAllByMemberId(Long memberId) {
        return listByMemberId(memberId);
    }

    default List<ChallengerRoleBasicInfo> findAllBasicByMemberId(Long memberId) {
        return listBasicByMemberId(memberId);
    }

    /**
     * @deprecated {@link #isCentralCoreInGisu}를 사용해주세요.
     */
    @Deprecated(since = "v1.5.0", forRemoval = true)
    boolean isCentralCore(Long memberId);

    /**
     * @deprecated {@link #isCentralMemberInGisu}를 사용해주세요.
     */
    @Deprecated(since = "v1.5.0", forRemoval = true)
    boolean isCentralMember(Long memberId);

    /**
     * @deprecated {@link #isSchoolCoreInGisu}를 사용해주세요.
     */
    @Deprecated(since = "v1.5.0", forRemoval = true)
    boolean isSchoolCore(Long memberId, Long schoolId);

    /**
     * @deprecated {@link #isSchoolAdminInGisu}를 사용해주세요.
     */
    @Deprecated(since = "v1.5.0", forRemoval = true)
    boolean isSchoolAdmin(Long memberId, Long schoolId);

    /**
     * @deprecated {@link #isChapterPresidentInGisu}를 사용해주세요.
     */
    @Deprecated(since = "v1.5.0", forRemoval = true)
    boolean isChapterPresident(Long memberId, Long chapterId);

    /**
     * RoleType만 가져오는게 아니라, 전체 {@link com.umc.product.authorization.domain.ChallengerRole}을 가져오도록 구성해서 사용해주세요.
     */
    @Deprecated(since = "v1.5.0", forRemoval = true)
    List<ChallengerRoleType> getAllRoleTypesByMemberIdAndGisuId(Long memberId, Long gisuId);

    /**
     * 특정 학교에서의 역할 타입 조회
     *
     * @param memberId 사용자 ID
     * @param schoolId 학교 ID
     * @return 해당 학교에서의 역할 타입 리스트
     */
    @Deprecated(since = "v1.5.0", forRemoval = true)
    List<ChallengerRoleType> getAllRoleTypesByMemberIdAndSchoolId(Long memberId, Long schoolId);

    /**
     * 여러 챌린저의 역할 타입을 일괄 조회
     *
     * @param challengerIds 챌린저 ID 목록
     * @return 챌린저 ID → 역할 타입 리스트
     */
    default Map<Long, List<ChallengerRoleType>> getAllRoleTypesByChallengerIds(Set<Long> challengerIds) {
        return mapRoleTypesByChallengerIds(challengerIds);
    }

    /**
     * 특정 기수에서 멤버가 파트장으로 담당하는 파트 목록을 조회합니다.
     * <p>
     * 기수 정보를 별도로 조회하지 않아 단일 쿼리로 동작합니다.
     *
     * @return responsiblePart가 설정된 역할에서 추출한 파트 Set. 없으면 빈 Set.
     */
    default Set<ChallengerPart> getAllResponsiblePartByMemberIdAndGisuId(Long memberId, Long gisuId) {
        return listResponsiblePartsByMemberIdAndGisuId(memberId, gisuId);
    }
}
