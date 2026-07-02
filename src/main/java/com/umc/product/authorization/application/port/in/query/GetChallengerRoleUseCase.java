package com.umc.product.authorization.application.port.in.query;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.umc.product.authorization.application.port.in.query.dto.ChallengerRoleInfo;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;

/**
 * 사용자의 역할 정보를 조회하는 UseCase
 * <p>
 * Notice, Curriculum 등 다른 도메인에서 사용자의 역할을 확인할 때 사용합니다.
 */
public interface GetChallengerRoleUseCase {

    ChallengerRoleInfo getById(Long challengerRoleId);

    List<ChallengerRoleInfo> findAllByMemberId(Long memberId);

    /**
     * 슈퍼어드민 여부를 확인합니다. 기수와 무관하게 모든 권한을 가집니다.
     *
     * @param memberId 사용자 ID
     * @return 슈퍼어드민 역할을 보유하면 true
     */
    boolean isSuperAdmin(Long memberId);

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
     * 특정 기수에서 특정 역할을 가지고 있는지 확인
     *
     * @param memberId 사용자 ID
     * @param gisuId   기수 ID
     * @param role     확인할 역할
     * @return 해당 기수에서 역할이 있으면 true
     */
    boolean hasRoleTypeInGisu(Long memberId, Long gisuId, ChallengerRoleType role);

    /**
     * 특정 기수에서 여러 역할 중 하나라도 가지고 있는지 확인
     *
     * @param memberId 사용자 ID
     * @param gisuId   기수 ID
     * @param roles    확인할 역할들
     * @return 해당 기수에서 역할 중 하나라도 있으면 true
     */
    boolean hasAnyRoleTypeInGisu(Long memberId, Long gisuId, ChallengerRoleType... roles);

    /**
     * 특정 기수에서 모든 역할을 가지고 있는지 확인
     *
     * @param memberId 사용자 ID
     * @param gisuId   기수 ID
     * @param roles    확인할 역할들
     * @return 해당 기수에서 모든 역할을 가지고 있으면 true
     */
    boolean hasAllRoleTypeInGisu(Long memberId, Long gisuId, ChallengerRoleType... roles);

    /**
     * 특정 기수에서 중앙운영사무국 총괄단 여부 (총괄, 부총괄)
     *
     * @param memberId 사용자 ID
     * @param gisuId   기수 ID
     * @return 해당 기수에서 총괄단이면 true
     */
    boolean isCentralCoreInGisu(Long memberId, Long gisuId);

    /**
     * 특정 기수에서 중앙운영사무국 멤버 여부 (총괄단, 운영국원, 교육국원)
     *
     * @param memberId 사용자 ID
     * @param gisuId   기수 ID
     * @return 해당 기수에서 중앙 멤버면 true
     */
    boolean isCentralMemberInGisu(Long memberId, Long gisuId);

    /**
     * 특정 기수에서 특정 학교의 회장단 여부 (회장, 부회장)
     *
     * @param memberId 사용자 ID
     * @param gisuId   기수 ID
     * @param schoolId 학교 ID
     * @return 해당 기수에서 해당 학교 회장단이면 true
     */
    boolean isSchoolCoreInGisu(Long memberId, Long gisuId, Long schoolId);

    /**
     * 특정 기수에서 특정 학교의 관리자 여부 (회장단, 파트장, 기타 운영진)
     *
     * @param memberId 사용자 ID
     * @param gisuId   기수 ID
     * @param schoolId 학교 ID
     * @return 해당 기수에서 해당 학교 관리자면 true
     */
    boolean isSchoolAdminInGisu(Long memberId, Long gisuId, Long schoolId);

    /**
     * 특정 기수에서 특정 지부의 지부장 여부
     *
     * @param memberId  사용자 ID
     * @param gisuId    기수 ID
     * @param chapterId 지부 ID
     * @return 해당 기수에서 해당 지부 지부장이면 true
     */
    boolean isChapterPresidentInGisu(Long memberId, Long gisuId, Long chapterId);

    /**
     * 여러 챌린저의 역할 타입을 일괄 조회
     *
     * @param challengerIds 챌린저 ID 목록
     * @return 챌린저 ID → 역할 타입 리스트
     */
    Map<Long, List<ChallengerRoleType>> getAllRoleTypesByChallengerIds(Set<Long> challengerIds);

    /**
     * 특정 기수에서 멤버가 파트장으로 담당하는 파트 목록을 조회합니다.
     * <p>
     * 기수 정보를 별도로 조회하지 않아 단일 쿼리로 동작합니다.
     *
     * @return responsiblePart가 설정된 역할에서 추출한 파트 Set. 없으면 빈 Set.
     */
    Set<ChallengerPart> getAllResponsiblePartByMemberIdAndGisuId(Long memberId, Long gisuId);

    /**
     * 특정 챌린저가 특정 기수·조직에서 동일 역할을 이미 보유하고 있는지 확인
     * <p>
     * 역할 중복 등록을 사전에 차단(fail-fast)하기 위해 사용합니다.
     *
     * @param challengerId   챌린저 ID
     * @param roleType       역할 타입
     * @param organizationId 조직 ID (CENTRAL 이면 null)
     * @param gisuId         기수 ID
     * @return 동일 역할이 이미 있으면 true
     */
    boolean hasRoleInOrganization(Long challengerId, ChallengerRoleType roleType, Long organizationId, Long gisuId);
}
