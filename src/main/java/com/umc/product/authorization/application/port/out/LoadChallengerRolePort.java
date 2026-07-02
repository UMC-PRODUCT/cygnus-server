package com.umc.product.authorization.application.port.out;

import com.umc.product.authorization.domain.ChallengerRole;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Challenger의 Role 정보를 조회하는 Port
 */
public interface LoadChallengerRolePort {

    /**
     * 사용자의 모든 Role 조회
     *
     * @param memberId 사용자 ID
     * @return Role 리스트 (Role이 없으면 빈 리스트)
     */
    List<ChallengerRole> findByMemberId(Long memberId);

    /**
     * 특정 기수에서의 사용자 Role 조회
     *
     * @param memberId 사용자 ID
     * @param gisuId   기수 ID
     * @return Role 리스트
     */
    List<ChallengerRole> findRolesByMemberIdAndGisuId(Long memberId, Long gisuId);

    /**
     * 여러 챌린저의 Role 일괄 조회
     *
     * @param challengerIds 챌린저 ID 목록
     * @return Role 리스트
     */
    List<ChallengerRole> findByChallengerIdIn(Set<Long> challengerIds);

    /**
     * ID로 ChallengerRole 조회
     */
    Optional<ChallengerRole> findById(Long id);

    /**
     * ID로 ChallengerRole 조회 - 없으면 예외
     */
    ChallengerRole getById(Long id);

    /**
     * 특정 챌린저가 특정 기수·조직에서 동일 역할을 이미 보유하고 있는지 여부
     *
     * @param challengerId   챌린저 ID
     * @param roleType       역할 타입
     * @param organizationId 조직 ID (CENTRAL 이면 null)
     * @param gisuId         기수 ID
     * @return 동일 역할이 이미 있으면 true
     */
    boolean existsByChallengerRole(Long challengerId, ChallengerRoleType roleType, Long organizationId, Long gisuId);
}
