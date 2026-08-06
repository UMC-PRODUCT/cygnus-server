package com.umc.product.member.application.port.in.query;

import java.util.List;

import com.umc.product.member.application.port.in.query.dto.MemberSystemRoleInfo;

public interface ListMemberSystemRoleUseCase {

    List<MemberSystemRoleInfo> listByMemberId(Long memberId);
}
