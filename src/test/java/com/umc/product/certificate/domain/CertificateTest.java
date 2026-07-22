package com.umc.product.certificate.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CertificateTest {

    private static final Instant ISSUED_AT = Instant.parse("2026-07-01T00:00:00Z");

    @Test
    @DisplayName("인증서는 발급일 기준 365일간 유효하다")
    void 인증서는_발급일_기준_365일간_유효하다() {
        // given
        Certificate certificate = Certificate.issue(spec());

        // when
        Instant lastValidInstant = ISSUED_AT.plus(365, ChronoUnit.DAYS).minusMillis(1);
        Instant expiredInstant = ISSUED_AT.plus(365, ChronoUnit.DAYS);

        // then
        assertThat(certificate.getExpiresAt()).isEqualTo(ISSUED_AT.plus(365, ChronoUnit.DAYS));
        assertThat(certificate.isValidAt(lastValidInstant)).isTrue();
        assertThat(certificate.isValidAt(expiredInstant)).isFalse();
    }

    @Test
    @DisplayName("폐기된 인증서는 만료 전에도 유효하지 않다")
    void 폐기된_인증서는_만료_전에도_유효하지_않다() {
        // given
        Certificate certificate = Certificate.issue(spec());

        // when
        certificate.revoke(99L, ISSUED_AT.plus(1, ChronoUnit.DAYS), "오발급");

        // then
        assertThat(certificate.getStatus()).isEqualTo(CertificateStatus.REVOKED);
        assertThat(certificate.isValidAt(ISSUED_AT.plus(2, ChronoUnit.DAYS))).isFalse();
        assertThat(certificate.getRevokedByMemberId()).isEqualTo(99L);
        assertThat(certificate.statusAt(ISSUED_AT.plus(2, ChronoUnit.DAYS)))
            .isEqualTo(CertificateStatus.REVOKED);
    }

    @Test
    @DisplayName("이미 폐기된 인증서는 다시 폐기할 수 없다")
    void 이미_폐기된_인증서는_다시_폐기할_수_없다() {
        // given
        Certificate certificate = Certificate.issue(spec());
        certificate.revoke(99L, ISSUED_AT.plus(1, ChronoUnit.DAYS), "오발급");

        // when & then
        assertThatThrownBy(() -> certificate.revoke(100L, ISSUED_AT.plus(2, ChronoUnit.DAYS), "재폐기"))
            .isInstanceOf(com.umc.product.certificate.domain.exception.CertificateException.class);
    }

    private CertificateIssueSpec spec() {
        return CertificateIssueSpec.builder()
            .serialNumber("UMC-CMP-20260701-ABCDEFGH")
            .template(CertificateTemplate.UMC_COURSE_COMPLETION)
            .recipientMemberId(1L)
            .recipientName("김유엠")
            .recipientSchoolName("유엠씨대학교")
            .gisuId(7L)
            .gisuGeneration(7L)
            .meritTitle(null)
            .meritDescription(null)
            .issuedByMemberId(10L)
            .issuedAt(ISSUED_AT)
            .fileId("file-id")
            .fileSha256("a".repeat(64))
            .build();
    }
}
