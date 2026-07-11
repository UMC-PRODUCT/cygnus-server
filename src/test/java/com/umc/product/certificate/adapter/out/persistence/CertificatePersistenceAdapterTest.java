package com.umc.product.certificate.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.umc.product.certificate.domain.Certificate;
import com.umc.product.certificate.domain.CertificateIssueSpec;
import com.umc.product.certificate.domain.CertificateTemplate;
import com.umc.product.support.PersistenceAdapterTest;

@PersistenceAdapterTest
@Import(CertificatePersistenceAdapter.class)
@DisplayName("CertificatePersistenceAdapter")
class CertificatePersistenceAdapterTest {

    private static final Instant NOW = Instant.parse("2026-07-01T00:00:00Z");

    @Autowired
    TestEntityManager em;

    @Autowired
    CertificatePersistenceAdapter sut;

    @Test
    @DisplayName("인증서 발급 범위에 PostgreSQL transaction advisory lock을 획득한다")
    void 인증서_발급_범위에_PostgreSQL_transaction_advisory_lock을_획득한다() {
        assertThatCode(() -> sut.lockScope(
            CertificateTemplate.UMC_COURSE_COMPLETION,
            1L,
            7L,
            null
        )).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("동일 수신자와 기수라도 템플릿이 다르면 별도 인증서로 조회한다")
    void 동일_수신자와_기수라도_템플릿이_다르면_별도_인증서로_조회한다() {
        // given
        Certificate course = persist(
            CertificateTemplate.UMC_COURSE_COMPLETION,
            "UMC-CMP-20260701-COURSE01",
            NOW.minus(2, ChronoUnit.DAYS),
            null
        );
        Certificate hackathon = persist(
            CertificateTemplate.UMC_HACKATHON_CERTIFICATION_OF_COMPLETION,
            "UMC-CMP-20260701-HACK0001",
            NOW.minus(1, ChronoUnit.DAYS),
            null
        );
        em.flush();
        em.clear();

        // when & then
        assertThat(sut.findValidByScope(
            CertificateTemplate.UMC_COURSE_COMPLETION,
            1L,
            7L,
            null,
            NOW
        )).get().extracting(Certificate::getSerialNumber).isEqualTo(course.getSerialNumber());
        assertThat(sut.findValidByScope(
            CertificateTemplate.UMC_HACKATHON_CERTIFICATION_OF_COMPLETION,
            1L,
            7L,
            null,
            NOW
        )).get().extracting(Certificate::getSerialNumber).isEqualTo(hackathon.getSerialNumber());
    }

    @Test
    @DisplayName("폐기되거나 만료된 인증서는 유효 범위 조회에서 제외한다")
    void 폐기되거나_만료된_인증서는_유효_범위_조회에서_제외한다() {
        // given
        Certificate valid = persist(
            CertificateTemplate.UMC_DEMO_DAY_GRAND_PRIZE,
            "UMC-MRT-20260701-VALID001",
            NOW.minus(1, ChronoUnit.DAYS),
            "대상"
        );
        Certificate revoked = persist(
            CertificateTemplate.UMC_DEMO_DAY_GRAND_PRIZE,
            "UMC-MRT-20260701-REVOKE01",
            NOW,
            "대상"
        );
        revoked.revoke(99L, NOW, "오발급");
        persist(
            CertificateTemplate.UMC_DEMO_DAY_GRAND_PRIZE,
            "UMC-MRT-20250701-EXPIRE01",
            NOW.minus(366, ChronoUnit.DAYS),
            "대상"
        );
        em.flush();
        em.clear();

        // when
        var result = sut.findValidByScope(
            CertificateTemplate.UMC_DEMO_DAY_GRAND_PRIZE,
            1L,
            7L,
            "대상",
            NOW
        );

        // then
        assertThat(result).get().extracting(Certificate::getSerialNumber).isEqualTo(valid.getSerialNumber());
    }

    private Certificate persist(
        CertificateTemplate template,
        String serialNumber,
        Instant issuedAt,
        String meritTitle
    ) {
        return em.persist(Certificate.issue(CertificateIssueSpec.builder()
            .serialNumber(serialNumber)
            .template(template)
            .recipientMemberId(1L)
            .recipientName("김유엠")
            .recipientSchoolName("유엠씨대학교")
            .gisuId(7L)
            .gisuGeneration(7L)
            .meritTitle(meritTitle)
            .issuedByMemberId(99L)
            .issuedAt(issuedAt)
            .fileId("file-" + serialNumber)
            .fileSha256("a".repeat(64))
            .build()));
    }
}
