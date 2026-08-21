package com.umc.product.test.application.port.out;

import com.umc.product.member.domain.MemberSystemRoleType;

public interface AssignSeedMemberSystemRolePort {

    boolean assignIfAbsent(Long memberId, MemberSystemRoleType roleType);
}
