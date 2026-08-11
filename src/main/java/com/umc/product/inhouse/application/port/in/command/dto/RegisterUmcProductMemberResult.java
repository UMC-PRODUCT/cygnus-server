package com.umc.product.inhouse.application.port.in.command.dto;

public record RegisterUmcProductMemberResult(
    Long umcProductMemberId,
    Long memberId,
    String email,
    String temporaryPassword
) {
    @Override
    public String toString() {
        return "RegisterUmcProductMemberResult[umcProductMemberId=" + umcProductMemberId
            + ", memberId=" + memberId
            + ", email=" + email
            + ", temporaryPassword=***]";
    }
}
