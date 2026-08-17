package com.umc.product.demoday.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.demoday.application.port.out.DemodayVoteQrTokenClaims;
import com.umc.product.demoday.application.port.out.VerifyDemodayVoteQrCredentialPort;
import com.umc.product.demoday.domain.enums.DemodayVoteQrPurpose;
import com.umc.product.demoday.domain.exception.DemodayDomainException;
import com.umc.product.demoday.domain.exception.DemodayErrorCode;

@ExtendWith(MockitoExtension.class)
@DisplayName("데모데이 INFO QR credential 검증")
class DemodayVoteQrCredentialValidatorTest {

    private static final Long POLL_ID = 1L;
    private static final String TOKEN = "token-value";
    private static final Instant ISSUED_AT = Instant.parse("2026-08-17T15:00:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2026-08-17T16:00:00Z");

    @Mock
    private VerifyDemodayVoteQrCredentialPort verifyDemodayVoteQrCredentialPort;

    @InjectMocks
    private DemodayVoteQrCredentialValidator validator;

    @Test
    @DisplayName("용도와 Poll이 모두 일치하면 통과한다")
    void validatePassesWhenPurposeAndPollMatch() {
        // given
        given(verifyDemodayVoteQrCredentialPort.verify(TOKEN)).willReturn(
            new DemodayVoteQrTokenClaims(DemodayVoteQrPurpose.VOTE_AUTH.name(), POLL_ID, ISSUED_AT, EXPIRES_AT));

        // when & then
        assertThatCode(() -> validator.validate(POLL_ID, TOKEN)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("용도가 다르면 거부한다")
    void validateRejectsPurposeMismatch() {
        // given
        given(verifyDemodayVoteQrCredentialPort.verify(TOKEN)).willReturn(
            new DemodayVoteQrTokenClaims("OTHER_PURPOSE", POLL_ID, ISSUED_AT, EXPIRES_AT));

        // when & then
        assertThatThrownBy(() -> validator.validate(POLL_ID, TOKEN))
            .isInstanceOfSatisfying(DemodayDomainException.class, exception ->
                assertThat(exception.getBaseCode()).isEqualTo(DemodayErrorCode.DEMODAY_VOTE_QR_PURPOSE_MISMATCH));
    }

    @Test
    @DisplayName("token의 Poll이 요청한 Poll과 다르면 거부한다")
    void validateRejectsPollMismatch() {
        // given
        given(verifyDemodayVoteQrCredentialPort.verify(TOKEN)).willReturn(
            new DemodayVoteQrTokenClaims(DemodayVoteQrPurpose.VOTE_AUTH.name(), POLL_ID + 1, ISSUED_AT, EXPIRES_AT));

        // when & then
        assertThatThrownBy(() -> validator.validate(POLL_ID, TOKEN))
            .isInstanceOfSatisfying(DemodayDomainException.class, exception ->
                assertThat(exception.getBaseCode()).isEqualTo(DemodayErrorCode.DEMODAY_VOTE_QR_POLL_MISMATCH));
    }
}
