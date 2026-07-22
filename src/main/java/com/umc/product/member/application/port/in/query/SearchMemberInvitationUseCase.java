package com.umc.product.member.application.port.in.query;

import java.util.Set;

import com.umc.product.member.application.port.in.query.dto.MemberInvitationSearchResult;
import com.umc.product.member.application.port.in.query.dto.SearchMemberInvitationQuery;

/**
 * Community가 회원 저장소를 직접 참조하지 않고 초대 가능한 회원을 조회하기 위한 공개 계약입니다.
 */
public interface SearchMemberInvitationUseCase {

    MemberInvitationSearchResult search(SearchMemberInvitationQuery query);

    Set<Long> batchGetInvitableMemberIds(Set<Long> memberIds);
}
