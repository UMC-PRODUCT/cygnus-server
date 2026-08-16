package com.umc.product.authentication.application.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.umc.product.authentication.domain.exception.AuthenticationDomainException;
import com.umc.product.authentication.domain.exception.AuthenticationErrorCode;
import com.umc.product.member.application.port.in.query.GetMemberCredentialUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberCredentialInfo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SsoCredentialVerifier {

    private final PasswordEncoder passwordEncoder;
    private final GetMemberCredentialUseCase getMemberCredentialUseCase;
    private final CredentialRehashService rehashService;

    public Long verifyEmailPassword(String email, String rawPassword) {
        MemberCredentialInfo credential = getMemberCredentialUseCase.findCredentialByEmail(email)
            .orElseThrow(() -> new AuthenticationDomainException(AuthenticationErrorCode.INVALID_LOGIN_CREDENTIAL));

        if (!passwordEncoder.matches(rawPassword, credential.passwordHash())) {
            throw new AuthenticationDomainException(AuthenticationErrorCode.INVALID_LOGIN_CREDENTIAL);
        }

        rehashService.rehashIfNeeded(credential, rawPassword);
        return credential.memberId();
    }
}
