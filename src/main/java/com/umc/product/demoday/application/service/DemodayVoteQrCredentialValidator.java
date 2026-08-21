package com.umc.product.demoday.application.service;

import org.springframework.stereotype.Component;

import com.umc.product.demoday.application.port.out.DemodayVoteQrTokenClaims;
import com.umc.product.demoday.application.port.out.VerifyDemodayVoteQrCredentialPort;
import com.umc.product.demoday.domain.enums.DemodayVoteQrPurpose;
import com.umc.product.demoday.domain.exception.DemodayDomainException;
import com.umc.product.demoday.domain.exception.DemodayErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * INFO QR credential이 이 Poll의 투표 인증에 실제로 쓰일 수 있는지 검증한다.
 *
 * <p>서명·만료 검증은 {@link VerifyDemodayVoteQrCredentialPort}(암호 경계)가 맡고, 이 클래스는 그 결과로
 * 받은 claim이 용도·Poll 조건을 만족하는지 판단하는 애플리케이션 규칙을 담당한다.
 * VoteAuthorization 발급 흐름이 해당 클래스를 호출한다.
 */
@Component
@RequiredArgsConstructor
public class DemodayVoteQrCredentialValidator {

    private final VerifyDemodayVoteQrCredentialPort verifyDemodayVoteQrCredentialPort;

    public void validate(Long pollId, String token) {
        DemodayVoteQrTokenClaims claims = verifyDemodayVoteQrCredentialPort.verify(token);

        if (!DemodayVoteQrPurpose.VOTE_AUTH.name().equals(claims.purpose())) {
            throw new DemodayDomainException(DemodayErrorCode.DEMODAY_VOTE_QR_PURPOSE_MISMATCH);
        }

        if (!pollId.equals(claims.pollId())) {
            throw new DemodayDomainException(DemodayErrorCode.DEMODAY_VOTE_QR_POLL_MISMATCH);
        }
    }
}
