package com.umc.product.inhouse.adapter.in.web.dto.response;

import com.umc.product.inhouse.application.port.in.query.dto.UmcProductMemberInfo;

public record UmcProductMyProfileResponse(
    UmcProductMemberResponse profile,
    boolean canManage
) {
    public static UmcProductMyProfileResponse of(UmcProductMemberInfo profile, boolean canManage) {
        return new UmcProductMyProfileResponse(
            profile == null ? null : UmcProductMemberResponse.from(profile),
            canManage
        );
    }
}
