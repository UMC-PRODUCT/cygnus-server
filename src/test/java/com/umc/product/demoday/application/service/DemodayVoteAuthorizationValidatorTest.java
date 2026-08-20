package com.umc.product.demoday.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.demoday.application.port.in.query.participant.DemodayParticipantType;
import com.umc.product.demoday.application.port.in.query.participant.MemberDemodayParticipant;
import com.umc.product.demoday.application.port.out.DemodayVoteAuthorizationTokenClaims;
import com.umc.product.demoday.application.port.out.VerifyDemodayVoteAuthorizationPort;
import com.umc.product.demoday.domain.enums.DemodayVoteAuthorizationPurpose;
import com.umc.product.demoday.domain.exception.DemodayDomainException;
import com.umc.product.demoday.domain.exception.DemodayErrorCode;

@ExtendWith(MockitoExtension.class)
@DisplayName("데모데이 투표 권한 claim 검증")
class DemodayVoteAuthorizationValidatorTest {

    private static final Long MEMBER_ID = 10L;
    private static final Long POLL_ID = 20L;
    private static final Long BOOTH_ID = 30L;
    private static final String TOKEN = "vote-authorization-token";
    private static final Instant ISSUED_AT = Instant.parse("2026-08-20T01:00:00Z");

    @Mock
    private VerifyDemodayVoteAuthorizationPort verifyDemodayVoteAuthorizationPort;

    @InjectMocks
    private DemodayVoteAuthorizationValidator validator;

    @Test
    @DisplayName("용도·Poll·참여자가 모두 일치하면 부스가 결합된 claim을 반환한다")
    void returnClaimsWhenAllBindingsMatch() {
        // given
        given(verifyDemodayVoteAuthorizationPort.verify(TOKEN)).willReturn(claims(MEMBER_ID, POLL_ID));

        // when
        DemodayVoteAuthorizationTokenClaims result = validator.validate(
            POLL_ID, new MemberDemodayParticipant(MEMBER_ID), TOKEN);

        // then
        assertThat(result.boothId()).isEqualTo(BOOTH_ID);
    }

    @Test
    @DisplayName("다른 참여자에게 발급된 권한은 거부한다")
    void rejectOtherParticipantAuthorization() {
        // given
        given(verifyDemodayVoteAuthorizationPort.verify(TOKEN)).willReturn(claims(MEMBER_ID + 1, POLL_ID));

        // when & then
        assertThatThrownBy(() -> validator.validate(
            POLL_ID, new MemberDemodayParticipant(MEMBER_ID), TOKEN))
            .isInstanceOfSatisfying(DemodayDomainException.class, exception ->
                assertThat(exception.getBaseCode())
                    .isEqualTo(DemodayErrorCode.DEMODAY_VOTE_AUTHORIZATION_PARTICIPANT_MISMATCH));
    }

    @Test
    @DisplayName("다른 Poll에 발급된 권한은 거부한다")
    void rejectOtherPollAuthorization() {
        // given
        given(verifyDemodayVoteAuthorizationPort.verify(TOKEN)).willReturn(claims(MEMBER_ID, POLL_ID + 1));

        // when & then
        assertThatThrownBy(() -> validator.validate(
            POLL_ID, new MemberDemodayParticipant(MEMBER_ID), TOKEN))
            .isInstanceOfSatisfying(DemodayDomainException.class, exception ->
                assertThat(exception.getBaseCode()).isEqualTo(DemodayErrorCode.DEMODAY_VOTE_POLL_MISMATCH));
    }

    @Test
    @DisplayName("최종 투표 용도가 아닌 권한은 거부한다")
    void rejectOtherPurposeAuthorization() {
        // given
        DemodayVoteAuthorizationTokenClaims claims = new DemodayVoteAuthorizationTokenClaims(
            "OTHER_PURPOSE",
            DemodayParticipantType.MEMBER,
            MEMBER_ID,
            POLL_ID,
            BOOTH_ID,
            ISSUED_AT,
            ISSUED_AT.plusSeconds(300)
        );
        given(verifyDemodayVoteAuthorizationPort.verify(TOKEN)).willReturn(claims);

        // when & then
        assertThatThrownBy(() -> validator.validate(
            POLL_ID, new MemberDemodayParticipant(MEMBER_ID), TOKEN))
            .isInstanceOfSatisfying(DemodayDomainException.class, exception ->
                assertThat(exception.getBaseCode())
                    .isEqualTo(DemodayErrorCode.DEMODAY_VOTE_AUTHORIZATION_PURPOSE_MISMATCH));
    }

    private DemodayVoteAuthorizationTokenClaims claims(Long memberId, Long pollId) {
        return new DemodayVoteAuthorizationTokenClaims(
            DemodayVoteAuthorizationPurpose.CAST_VOTE.name(),
            DemodayParticipantType.MEMBER,
            memberId,
            pollId,
            BOOTH_ID,
            ISSUED_AT,
            ISSUED_AT.plusSeconds(300)
        );
    }
}
