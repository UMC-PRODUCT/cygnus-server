package com.umc.product.member.application.port.out;

import java.util.Set;

import com.umc.product.member.application.port.out.dto.MemberInvitationCandidatePage;
import com.umc.product.member.application.port.out.dto.SearchMemberInvitationCondition;

public interface SearchMemberInvitationPort {

    MemberInvitationCandidatePage search(SearchMemberInvitationCondition condition);

    Set<Long> findActiveMemberIds(Set<Long> memberIds);
}
