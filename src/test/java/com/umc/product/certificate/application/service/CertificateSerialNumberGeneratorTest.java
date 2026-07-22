package com.umc.product.certificate.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.SecureRandom;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.certificate.domain.CertificateTemplate;

class CertificateSerialNumberGeneratorTest {

    @Test
    @DisplayName("수료증 템플릿과 발급일을 포함한 일련번호를 생성한다")
    void 수료증_템플릿과_발급일을_포함한_일련번호를_생성한다() {
        // given
        CertificateSerialNumberGenerator generator = new CertificateSerialNumberGenerator(new SecureRandom());

        // when
        String serialNumber = generator.generate(CertificateTemplate.UMC_COURSE_COMPLETION,
            Instant.parse("2026-07-01T09:00:00Z"));

        // then
        assertThat(serialNumber).matches("UMC-CMP-20260701-[A-Z2-7]{8}");
    }

    @Test
    @DisplayName("공로증 일련번호는 MRT 코드를 사용한다")
    void 공로증_일련번호는_MRT_코드를_사용한다() {
        // given
        CertificateSerialNumberGenerator generator = new CertificateSerialNumberGenerator(new SecureRandom());

        // when
        String serialNumber = generator.generate(CertificateTemplate.UMC_DEMO_DAY_FIRST_PRIZE,
            Instant.parse("2026-07-01T09:00:00Z"));

        // then
        assertThat(serialNumber).matches("UMC-MRT-20260701-[A-Z2-7]{8}");
    }

    @Test
    @DisplayName("기본 생성자는 보안 난수로 일련번호를 생성한다")
    void 기본_생성자는_보안_난수로_일련번호를_생성한다() {
        CertificateSerialNumberGenerator generator = new CertificateSerialNumberGenerator();

        assertThat(generator.generate(
            CertificateTemplate.UMC_COURSE_COMPLETION,
            Instant.parse("2026-07-01T09:00:00Z")
        )).matches("UMC-CMP-20260701-[A-Z2-7]{8}");
    }
}
