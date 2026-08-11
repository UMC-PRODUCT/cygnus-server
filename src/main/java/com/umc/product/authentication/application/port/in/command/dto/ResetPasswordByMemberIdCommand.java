package com.umc.product.authentication.application.port.in.command.dto;

import com.umc.product.authentication.domain.CredentialPolicy;

public record ResetPasswordByMemberIdCommand(
    Long memberId,
    String newRawPassword
) {
    public ResetPasswordByMemberIdCommand {
        CredentialPolicy.validatePassword(newRawPassword);
    }

    @Override
    public String toString() {
        return "ResetPasswordByMemberIdCommand[memberId=" + memberId + ", newRawPassword=***]";
    }
}
