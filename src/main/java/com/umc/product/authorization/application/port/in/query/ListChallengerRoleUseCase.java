package com.umc.product.authorization.application.port.in.query;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.umc.product.authorization.application.port.in.query.dto.ChallengerRoleBasicInfo;
import com.umc.product.authorization.application.port.in.query.dto.ChallengerRoleInfo;
import com.umc.product.authorization.application.port.in.query.dto.ChallengerRolePolicyInfo;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;

/**
 * ChallengerRole 조회 전용 UseCase입니다.
 */
public interface ListChallengerRoleUseCase {

    ChallengerRoleInfo getById(Long challengerRoleId);

    List<ChallengerRoleInfo> listByMemberId(Long memberId);

    List<ChallengerRoleBasicInfo> listBasicByMemberId(Long memberId);

    List<ChallengerRolePolicyInfo> listPolicyFactsByMemberId(Long memberId);

    List<ChallengerRoleInfo> listByMemberIdAndGisuId(Long memberId, Long gisuId);

    Map<Long, List<ChallengerRoleType>> mapRoleTypesByChallengerIds(Set<Long> challengerIds);

    Set<ChallengerPart> listResponsiblePartsByMemberIdAndGisuId(Long memberId, Long gisuId);
}
