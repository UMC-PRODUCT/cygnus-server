package com.umc.product.member.application.port.in.query;

import java.util.Map;
import java.util.Set;

import com.umc.product.member.application.port.in.query.dto.ChallengerInvitationInfo;
import com.umc.product.member.application.port.in.query.dto.ChallengerInvitationSearchResult;
import com.umc.product.member.application.port.in.query.dto.SearchChallengerInvitationQuery;

/**
 * Community가 회원/챌린저 저장소를 직접 참조하지 않고
 * 초대 대상을 조회하기 위한 공개 계약입니다.
 */
public interface SearchChallengerInvitationUseCase {

    ChallengerInvitationSearchResult search(SearchChallengerInvitationQuery query);

    Map<Long, ChallengerInvitationInfo> batchGetEligibleChallengers(Set<Long> memberIds);
}
