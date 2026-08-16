package com.umc.product.demoday.application.service;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.port.in.query.CheckChallengerAuthorityUseCase;
import com.umc.product.demoday.domain.exception.DemodayDomainException;
import com.umc.product.demoday.domain.exception.DemodayErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DemodayAdminAccessChecker {

    private final CheckChallengerAuthorityUseCase authorityUseCase;

    public void validateAdminAccess(Long memberId, Long gisuId) {
        if (!authorityUseCase.isCentralCoreInGisu(memberId, gisuId)) {
            throw new DemodayDomainException(DemodayErrorCode.DEMODAY_ADMIN_ACCESS_DENIED);
        }
    }
}
