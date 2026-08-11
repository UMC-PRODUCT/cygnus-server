package com.umc.product.inhouse.adapter.in.web.dto.response;

import com.umc.product.inhouse.application.port.in.query.dto.UmcProductMemberAccountInfo;
import com.umc.product.inhouse.domain.enums.UmcProductMemberAccountType;

public record UmcProductMemberAccountResponse(
    Long memberId,
    String name,
    String nickname,
    String email,
    String profileImageLink,
    UmcProductMemberAccountType accountType
) {
    public static UmcProductMemberAccountResponse from(UmcProductMemberAccountInfo info) {
        return new UmcProductMemberAccountResponse(
            info.memberId(),
            info.name(),
            info.nickname(),
            info.email(),
            info.profileImageLink(),
            info.accountType()
        );
    }
}
