package com.umc.product.demoday.application.service;

import org.springframework.stereotype.Component;

import com.umc.product.demoday.application.port.in.query.participant.DemodayParticipant;
import com.umc.product.demoday.application.port.out.DemodayVoteAuthorizationTokenClaims;
import com.umc.product.demoday.application.port.out.VerifyDemodayVoteAuthorizationPort;
import com.umc.product.demoday.domain.enums.DemodayVoteAuthorizationPurpose;
import com.umc.product.demoday.domain.exception.DemodayDomainException;
import com.umc.product.demoday.domain.exception.DemodayErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DemodayVoteAuthorizationValidator {

    private final VerifyDemodayVoteAuthorizationPort verifyDemodayVoteAuthorizationPort;

    public DemodayVoteAuthorizationTokenClaims validate(
        Long pollId,
        DemodayParticipant participant,
        String token
    ) {
        DemodayVoteAuthorizationTokenClaims claims = verifyDemodayVoteAuthorizationPort.verify(token);

        if (!DemodayVoteAuthorizationPurpose.CAST_VOTE.name().equals(claims.purpose())) {
            throw new DemodayDomainException(DemodayErrorCode.DEMODAY_VOTE_AUTHORIZATION_PURPOSE_MISMATCH);
        }
        if (!pollId.equals(claims.pollId())) {
            throw new DemodayDomainException(DemodayErrorCode.DEMODAY_VOTE_POLL_MISMATCH);
        }
        if (participant.participantType() != claims.participantType()
            || !participant.participantId().equals(claims.participantId())) {
            throw new DemodayDomainException(DemodayErrorCode.DEMODAY_VOTE_AUTHORIZATION_PARTICIPANT_MISMATCH);
        }

        return claims;
    }
}
