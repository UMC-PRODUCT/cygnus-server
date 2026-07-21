package com.umc.product.member.application.port.out;

import java.util.List;

import com.umc.product.member.domain.MemberSystemRole;

public interface LoadMemberSystemRolePort {

    List<MemberSystemRole> listByMemberId(Long memberId);
}
