package com.umc.product.inhouse.application.port.in.query.dto;

import com.umc.product.inhouse.domain.enums.UmcProductMemberAccountType;

public record UmcProductMemberAccountInfo(
    Long memberId,
    String name,
    String nickname,
    String email,
    String profileImageLink,
    UmcProductMemberAccountType accountType
) {
}
