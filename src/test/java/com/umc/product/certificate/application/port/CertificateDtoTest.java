package com.umc.product.certificate.application.port;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.certificate.application.port.in.command.dto.RevokeCertificateCommand;
import com.umc.product.certificate.application.port.out.dto.CertificatePdfRenderCommand;

class CertificateDtoTest {

    private static final Instant NOW = Instant.parse("2026-07-01T00:00:00Z");

    @Test
    @DisplayName("PDF 렌더 명령은 일련번호를 발급번호 기본값으로 사용한다")
    void PDF_렌더_명령은_일련번호를_발급번호_기본값으로_사용한다() {
        CertificatePdfRenderCommand command = CertificatePdfRenderCommand.builder()
            .serialNumber("UMC-CMP-20260701-ABCDEFGH")
            .recipientName("김유엠")
            .gisuGeneration(7L)
            .issuedAt(NOW)
            .expiresAt(NOW.plusSeconds(1))
            .verificationUrl("/verify")
            .build();

        assertThat(command.issuanceNumber()).isEqualTo(command.serialNumber());
    }

    @Test
    @DisplayName("폐기 명령은 null 또는 blank 사유를 거부한다")
    void 폐기_명령은_null_또는_blank_사유를_거부한다() {
        assertThatThrownBy(() -> new RevokeCertificateCommand(1L, 2L, null))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RevokeCertificateCommand(1L, 2L, " "))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
