package com.umc.product.demoday.adapter.out;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.demoday.application.port.out.DemodayVoteQrTokenClaims;
import com.umc.product.demoday.config.DemodayVoteQrProperties;
import com.umc.product.demoday.domain.enums.DemodayVoteQrPurpose;
import com.umc.product.demoday.domain.exception.DemodayDomainException;
import com.umc.product.demoday.domain.exception.DemodayErrorCode;

class JwtDemodayVoteQrTokenVerifierTest {

    private static final String SIGNING_KEY = "unit-test-demoday-vote-qr-signing-key-needs-32-bytes-minimum";
    private static final String OTHER_SIGNING_KEY = "unit-test-other-demoday-vote-qr-signing-key-needs-32-bytes-min";
    private static final Long POLL_ID = 1L;
    private static final Instant WINDOW_START = Instant.parse("2026-08-17T15:00:00Z");
    private static final Instant WINDOW_END = Instant.parse("2026-08-17T16:00:00Z");

    private final JwtDemodayVoteQrTokenGenerator generator =
        new JwtDemodayVoteQrTokenGenerator(new DemodayVoteQrProperties(SIGNING_KEY));

    @Test
    @DisplayName("유효 구간 안에서 검증하면 발급 claim을 그대로 반환한다")
    void verifyValidTokenReturnsClaims() {
        // given
        String token = generator.generate(POLL_ID, WINDOW_START, WINDOW_END);
        JwtDemodayVoteQrTokenVerifier verifier = verifierAt(WINDOW_START.plusSeconds(1800));

        // when
        DemodayVoteQrTokenClaims claims = verifier.verify(token);

        // then
        assertThat(claims.purpose()).isEqualTo(DemodayVoteQrPurpose.VOTE_AUTH.name());
        assertThat(claims.pollId()).isEqualTo(POLL_ID);
        assertThat(claims.issuedAt()).isEqualTo(WINDOW_START);
        assertThat(claims.expiresAt()).isEqualTo(WINDOW_END);
    }

    @Test
    @DisplayName("구간 만료 후 60초까지는 clock skew로 허용한다")
    void verifyAllowsSixtySecondSkewAfterExpiry() {
        // given
        String token = generator.generate(POLL_ID, WINDOW_START, WINDOW_END);
        JwtDemodayVoteQrTokenVerifier verifier = verifierAt(WINDOW_END.plusSeconds(60));

        // when & then
        assertThat(verifier.verify(token).pollId()).isEqualTo(POLL_ID);
    }

    @Test
    @DisplayName("구간 만료 61초 뒤에는 만료로 거부한다")
    void verifyRejectsAfterSixtyOneSeconds() {
        // given
        String token = generator.generate(POLL_ID, WINDOW_START, WINDOW_END);
        JwtDemodayVoteQrTokenVerifier verifier = verifierAt(WINDOW_END.plusSeconds(61));

        // when & then
        assertThatThrownBy(() -> verifier.verify(token))
            .isInstanceOfSatisfying(DemodayDomainException.class,
                exception -> assertThat(exception.getBaseCode())
                    .isEqualTo(DemodayErrorCode.DEMODAY_VOTE_QR_EXPIRED));
    }

    @Test
    @DisplayName("다른 키로 서명되거나 변조된 token은 거부한다")
    void verifyRejectsTamperedSignature() {
        // given
        String tokenFromOtherKey = new JwtDemodayVoteQrTokenGenerator(
            new DemodayVoteQrProperties(OTHER_SIGNING_KEY))
            .generate(POLL_ID, WINDOW_START, WINDOW_END);
        JwtDemodayVoteQrTokenVerifier verifier = verifierAt(WINDOW_START.plusSeconds(1800));

        // when & then
        assertThatThrownBy(() -> verifier.verify(tokenFromOtherKey))
            .isInstanceOfSatisfying(DemodayDomainException.class,
                exception -> assertThat(exception.getBaseCode())
                    .isEqualTo(DemodayErrorCode.DEMODAY_VOTE_QR_INVALID));
    }

    @Test
    @DisplayName("형식이 깨진 token은 거부한다")
    void verifyRejectsMalformedToken() {
        // given
        JwtDemodayVoteQrTokenVerifier verifier = verifierAt(WINDOW_START.plusSeconds(1800));

        // when & then
        assertThatThrownBy(() -> verifier.verify("not-a-jwt"))
            .isInstanceOfSatisfying(DemodayDomainException.class,
                exception -> assertThat(exception.getBaseCode())
                    .isEqualTo(DemodayErrorCode.DEMODAY_VOTE_QR_INVALID));
    }

    private JwtDemodayVoteQrTokenVerifier verifierAt(Instant now) {
        return new JwtDemodayVoteQrTokenVerifier(
            new DemodayVoteQrProperties(SIGNING_KEY),
            Clock.fixed(now, ZoneOffset.UTC));
    }
}
