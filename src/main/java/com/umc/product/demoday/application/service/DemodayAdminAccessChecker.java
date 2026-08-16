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

    /**
     * 기수와 무관하게 시스템 관리자(SUPER_ADMIN)인지 검사한다.
     *
     * <p>{@link #validateAdminAccess(Long, Long)}는 해당 기수의 총괄단도 통과시키지만 이 검사는 통과시키지 않는다.
     * 한 번에 여러 행을 만드는 일괄 경로처럼, 실수의 파급이 크고 평시 운영 흐름에 속하지 않는 API에 쓴다.
     */
    public void validateSystemAdminAccess(Long memberId) {
        if (!authorityUseCase.isSuperAdmin(memberId)) {
            throw new DemodayDomainException(DemodayErrorCode.DEMODAY_ADMIN_ACCESS_DENIED);
        }
    }
}
