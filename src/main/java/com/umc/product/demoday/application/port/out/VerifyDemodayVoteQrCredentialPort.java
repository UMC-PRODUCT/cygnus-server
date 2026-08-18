package com.umc.product.demoday.application.port.out;

import com.umc.product.demoday.domain.exception.DemodayDomainException;

public interface VerifyDemodayVoteQrCredentialPort {

    /**
     * INFO QR credential의 서명과 만료 여부를 검증하고 claim을 반환한다.
     *
     * <p>서명이 유효하지 않거나(변조·형식 오류) 허용된 clock skew를 넘겨 만료됐으면 {@link DemodayDomainException}을 던진다.
     * 용도(purpose)·Poll 일치 여부는 이 Port의 책임이 아니다 — 호출자가 반환된 claim으로 판단한다.
     */
    DemodayVoteQrTokenClaims verify(String token);
}
