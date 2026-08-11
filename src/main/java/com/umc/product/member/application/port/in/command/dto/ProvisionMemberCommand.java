package com.umc.product.member.application.port.in.command.dto;

import com.umc.product.authentication.domain.CredentialPolicy;

public record ProvisionMemberCommand(
    String name,
    String nickname,
    String email,
    Long schoolId,
    String rawPassword
) {
    public ProvisionMemberCommand {
        CredentialPolicy.validateEmail(email);
        CredentialPolicy.validatePassword(rawPassword);
    }

    @Override
    public String toString() {
        return "ProvisionMemberCommand[name=" + name
            + ", nickname=" + nickname
            + ", email=" + email
            + ", schoolId=" + schoolId
            + ", rawPassword=***]";
    }
}
