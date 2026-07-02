package com.umc.product.certificate.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.SecureRandom;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.certificate.domain.CertificateType;

class CertificateSerialNumberGeneratorTest {

    @Test
    @DisplayName("인증서 타입과 발급일을 포함한 일련번호를 생성한다")
    void 인증서_타입과_발급일을_포함한_일련번호를_생성한다() {
        // given
        CertificateSerialNumberGenerator generator = new CertificateSerialNumberGenerator(new SecureRandom());

        // when
        String serialNumber = generator.generate(CertificateType.PROJECT_PARTICIPATION,
            Instant.parse("2026-07-01T09:00:00Z"));

        // then
        assertThat(serialNumber).matches("UMC-PRJ-20260701-[A-Z2-7]{8}");
    }

    @Test
    @DisplayName("공로증 일련번호는 MRT 코드를 사용한다")
    void 공로증_일련번호는_MRT_코드를_사용한다() {
        // given
        CertificateSerialNumberGenerator generator = new CertificateSerialNumberGenerator(new SecureRandom());

        // when
        String serialNumber = generator.generate(CertificateType.MERIT,
            Instant.parse("2026-07-01T09:00:00Z"));

        // then
        assertThat(serialNumber).matches("UMC-MRT-20260701-[A-Z2-7]{8}");
    }
}
