package com.umc.product.demoday.adapter.out;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.demoday.application.port.in.query.participant.MemberDemodayParticipant;
import com.umc.product.demoday.application.port.out.DemodayVoteAuthorizationTokenClaims;
import com.umc.product.demoday.config.DemodayVoteAuthorizationProperties;
import com.umc.product.demoday.config.DemodayVoteQrProperties;
import com.umc.product.demoday.domain.enums.DemodayVoteAuthorizationPurpose;
import com.umc.product.demoday.domain.exception.DemodayDomainException;
import com.umc.product.demoday.domain.exception.DemodayErrorCode;

@DisplayName("데모데이 투표 권한 JWT")
class JwtDemodayVoteAuthorizationTest {

    private static final String SIGNING_KEY =
        "unit-test-demoday-vote-authorization-signing-key-needs-32-bytes";
    private static final String INFO_QR_SIGNING_KEY =
        "unit-test-demoday-info-qr-separate-signing-key-needs-32-bytes";
    private static final Long MEMBER_ID = 10L;
    private static final Long POLL_ID = 20L;
    private static final Long BOOTH_ID = 30L;
    private static final Instant ISSUED_AT = Instant.parse("2026-08-20T01:00:00Z");
    private static final Instant EXPIRES_AT = ISSUED_AT.plusSeconds(300);

    private final JwtDemodayVoteAuthorizationGenerator generator =
        new JwtDemodayVoteAuthorizationGenerator(new DemodayVoteAuthorizationProperties(SIGNING_KEY));

    @Test
    @DisplayName("참여자·Poll·부스·5분 만료 정보를 서명해 다시 검증한다")
    void generateAndVerifyBoundClaims() {
        // given
        String token = generate();
        JwtDemodayVoteAuthorizationVerifier verifier = verifierAt(ISSUED_AT.plusSeconds(60));

        // when
        DemodayVoteAuthorizationTokenClaims claims = verifier.verify(token);

        // then
        assertThat(claims.purpose()).isEqualTo(DemodayVoteAuthorizationPurpose.CAST_VOTE.name());
        assertThat(claims.participantId()).isEqualTo(MEMBER_ID);
        assertThat(claims.participantType().name()).isEqualTo("MEMBER");
        assertThat(claims.pollId()).isEqualTo(POLL_ID);
        assertThat(claims.boothId()).isEqualTo(BOOTH_ID);
        assertThat(claims.issuedAt()).isEqualTo(ISSUED_AT);
        assertThat(claims.expiresAt()).isEqualTo(EXPIRES_AT);
    }

    @Test
    @DisplayName("만료 시각부터 추가 유예 없이 거부한다")
    void rejectAtExactExpirationWithoutClockSkew() {
        // given
        String token = generate();

        // when & then
        assertThatThrownBy(() -> verifierAt(EXPIRES_AT).verify(token))
            .isInstanceOfSatisfying(DemodayDomainException.class, exception ->
                assertThat(exception.getBaseCode())
                    .isEqualTo(DemodayErrorCode.DEMODAY_VOTE_AUTHORIZATION_EXPIRED));
    }

    @Test
    @DisplayName("INFO QR 키로는 투표 권한 token을 검증할 수 없다")
    void infoQrKeyCannotVerifyVoteAuthorization() {
        // given
        String token = generate();
        JwtDemodayVoteQrTokenVerifier infoQrVerifier = new JwtDemodayVoteQrTokenVerifier(
            new DemodayVoteQrProperties(INFO_QR_SIGNING_KEY),
            Clock.fixed(ISSUED_AT.plusSeconds(60), ZoneOffset.UTC));

        // when & then
        assertThatThrownBy(() -> infoQrVerifier.verify(token))
            .isInstanceOfSatisfying(DemodayDomainException.class, exception ->
                assertThat(exception.getBaseCode()).isEqualTo(DemodayErrorCode.DEMODAY_VOTE_QR_INVALID));
    }

    @Test
    @DisplayName("형식이 깨진 token은 유효하지 않은 투표 권한으로 거부한다")
    void rejectMalformedToken() {
        assertThatThrownBy(() -> verifierAt(ISSUED_AT).verify("not-a-jwt"))
            .isInstanceOfSatisfying(DemodayDomainException.class, exception ->
                assertThat(exception.getBaseCode())
                    .isEqualTo(DemodayErrorCode.DEMODAY_VOTE_AUTHORIZATION_INVALID));
    }

    @Test
    @DisplayName("INFO QR과 투표 권한에 같은 signing key를 설정하면 시작을 거부한다")
    void rejectSharedSigningKeyConfiguration() {
        assertThatThrownBy(() -> new JwtDemodayVoteAuthorizationVerifier(
            new DemodayVoteAuthorizationProperties(SIGNING_KEY),
            new DemodayVoteQrProperties(SIGNING_KEY),
            Clock.fixed(ISSUED_AT, ZoneOffset.UTC)
        )).isInstanceOf(IllegalArgumentException.class);
    }

    private String generate() {
        return generator.generate(
            new MemberDemodayParticipant(MEMBER_ID), POLL_ID, BOOTH_ID, ISSUED_AT, EXPIRES_AT);
    }

    private JwtDemodayVoteAuthorizationVerifier verifierAt(Instant now) {
        return new JwtDemodayVoteAuthorizationVerifier(
            new DemodayVoteAuthorizationProperties(SIGNING_KEY),
            new DemodayVoteQrProperties(INFO_QR_SIGNING_KEY),
            Clock.fixed(now, ZoneOffset.UTC));
    }
}
