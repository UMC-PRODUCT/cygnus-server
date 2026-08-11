package com.umc.product.inhouse.adapter.in.web.dto.response;

import com.umc.product.inhouse.application.port.in.command.dto.RegisterUmcProductMemberResult;

public record RegisterUmcProductMemberResponse(
    Long umcProductMemberId,
    Long memberId,
    String email,
    String temporaryPassword
) {
    public static RegisterUmcProductMemberResponse from(RegisterUmcProductMemberResult result) {
        return new RegisterUmcProductMemberResponse(
            result.umcProductMemberId(),
            result.memberId(),
            result.email(),
            result.temporaryPassword()
        );
    }

    @Override
    public String toString() {
        return "RegisterUmcProductMemberResponse[umcProductMemberId=" + umcProductMemberId
            + ", memberId=" + memberId
            + ", email=" + email
            + ", temporaryPassword=***]";
    }
}
