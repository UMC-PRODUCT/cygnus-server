package com.umc.product.certificate.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.umc.product.certificate.domain.Certificate;
import com.umc.product.certificate.domain.CertificateIssueSpec;
import com.umc.product.certificate.domain.CertificateTemplate;
import com.umc.product.certificate.domain.exception.CertificateException;
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

    @Autowired
    PlatformTransactionManager transactionManager;

    @Test
    @DisplayName("인증서 발급 범위에 PostgreSQL transaction advisory lock을 획득한다")
    void 인증서_발급_범위에_PostgreSQL_transaction_advisory_lock을_획득한다() {
        assertThatCode(() -> {
            sut.lockScope(
            CertificateTemplate.UMC_COURSE_COMPLETION,
            1L,
            7L,
            null
            );
            sut.lockScope(
                CertificateTemplate.UMC_DEMO_DAY_GRAND_PRIZE,
                1L,
                7L,
                "대상"
            );
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("동일 인증서 발급 범위의 transaction advisory lock은 첫 트랜잭션 종료까지 대기한다")
    void 동일_인증서_발급_범위의_transaction_advisory_lock은_첫_트랜잭션_종료까지_대기한다() throws Exception {
        // given
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        CountDownLatch firstAcquired = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        CountDownLatch secondAttempted = new CountDownLatch(1);
        CountDownLatch secondAcquired = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<?> first = executor.submit(() -> transactionTemplate.executeWithoutResult(status -> {
                lockCompletionScope();
                firstAcquired.countDown();
                await(releaseFirst);
            }));
            assertThat(firstAcquired.await(5, TimeUnit.SECONDS)).isTrue();

            Future<?> second = executor.submit(() -> transactionTemplate.executeWithoutResult(status -> {
                secondAttempted.countDown();
                lockCompletionScope();
                secondAcquired.countDown();
            }));
            assertThat(secondAttempted.await(5, TimeUnit.SECONDS)).isTrue();

            // when & then
            assertThat(secondAcquired.await(300, TimeUnit.MILLISECONDS)).isFalse();
            releaseFirst.countDown();
            first.get(5, TimeUnit.SECONDS);
            assertThat(secondAcquired.await(5, TimeUnit.SECONDS)).isTrue();
            second.get(5, TimeUnit.SECONDS);
        } finally {
            releaseFirst.countDown();
            executor.shutdownNow();
        }
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

    @Test
    @DisplayName("저장 및 단건·목록 조회 port 계약을 repository에 위임한다")
    void 저장_및_단건_목록_조회_port_계약을_repository에_위임한다() {
        // given
        Certificate certificate = Certificate.issue(spec(
            CertificateTemplate.UMC_COURSE_COMPLETION,
            "UMC-CMP-20260701-PORT0001",
            NOW,
            null
        ));

        // when
        Certificate saved = sut.save(certificate);
        em.flush();
        em.clear();

        // then
        assertThat(sut.findById(saved.getId())).isPresent();
        assertThat(sut.getById(saved.getId()).getSerialNumber()).isEqualTo(saved.getSerialNumber());
        assertThat(sut.findBySerialNumber(saved.getSerialNumber())).isPresent();
        assertThat(sut.existsBySerialNumber(saved.getSerialNumber())).isTrue();
        assertThat(sut.listByRecipientMemberId(1L))
            .extracting(Certificate::getSerialNumber)
            .contains(saved.getSerialNumber());
    }

    @Test
    @DisplayName("필수 조회 대상 인증서가 없으면 not-found 예외를 던진다")
    void 필수_조회_대상_인증서가_없으면_not_found_예외를_던진다() {
        assertThatThrownBy(() -> sut.getById(Long.MAX_VALUE))
            .isInstanceOf(CertificateException.class);
    }

    private Certificate persist(
        CertificateTemplate template,
        String serialNumber,
        Instant issuedAt,
        String meritTitle
    ) {
        return em.persist(Certificate.issue(spec(template, serialNumber, issuedAt, meritTitle)));
    }

    private CertificateIssueSpec spec(
        CertificateTemplate template,
        String serialNumber,
        Instant issuedAt,
        String meritTitle
    ) {
        return CertificateIssueSpec.builder()
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
            .build();
    }

    private void lockCompletionScope() {
        sut.lockScope(CertificateTemplate.UMC_COURSE_COMPLETION, 1L, 7L, null);
    }

    private void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("lock wait timed out");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("lock wait interrupted", e);
        }
    }
}
