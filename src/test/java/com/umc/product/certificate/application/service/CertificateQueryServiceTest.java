package com.umc.product.certificate.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.certificate.application.port.in.query.dto.CertificateVerificationInfo;
import com.umc.product.certificate.application.port.out.LoadCertificatePort;
import com.umc.product.certificate.domain.Certificate;
import com.umc.product.certificate.domain.CertificateIssueSpec;
import com.umc.product.certificate.domain.CertificateIssuer;
import com.umc.product.certificate.domain.CertificateType;
import com.umc.product.storage.application.port.in.query.GetFileUseCase;

@ExtendWith(MockitoExtension.class)
class CertificateQueryServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-01T00:00:00Z");

    @Mock
    LoadCertificatePort loadCertificatePort;

    @Mock
    GetFileUseCase getFileUseCase;

    @Test
    @DisplayName("존재하지 않는 일련번호 검증은 200 응답용 invalid 결과를 반환한다")
    void 존재하지_않는_일련번호_검증은_invalid_결과를_반환한다() {
        // given
        given(loadCertificatePort.findBySerialNumber("missing")).willReturn(Optional.empty());
        CertificateQueryService sut = new CertificateQueryService(
            loadCertificatePort,
            getFileUseCase,
            Clock.fixed(NOW, ZoneOffset.UTC)
        );

        // when
        CertificateVerificationInfo result = sut.verifyBySerialNumber("missing");

        // then
        assertThat(result.valid()).isFalse();
        assertThat(result.status()).isEqualTo("NOT_FOUND");
    }

    @Test
    @DisplayName("공개 검증 결과는 이름을 마스킹하고 만료 상태를 계산한다")
    void 공개_검증_결과는_이름을_마스킹하고_만료_상태를_계산한다() {
        // given
        Certificate expired = Certificate.issue(CertificateIssueSpec.builder()
            .serialNumber("UMC-CMP-20250701-ABCDEFGH")
            .type(CertificateType.COMPLETION)
            .issuer(CertificateIssuer.UNIVERSITY_MAKEUS_CHALLENGE)
            .recipientMemberId(1L)
            .recipientName("김유엠")
            .recipientSchoolName("유엠씨대학교")
            .gisuId(7L)
            .gisuGeneration(7L)
            .issuedByMemberId(10L)
            .issuedAt(NOW.minus(365, ChronoUnit.DAYS))
            .fileId("file-id")
            .fileSha256("b".repeat(64))
            .build());
        given(loadCertificatePort.findBySerialNumber(expired.getSerialNumber()))
            .willReturn(Optional.of(expired));
        CertificateQueryService sut = new CertificateQueryService(
            loadCertificatePort,
            getFileUseCase,
            Clock.fixed(NOW, ZoneOffset.UTC)
        );

        // when
        CertificateVerificationInfo result = sut.verifyBySerialNumber(expired.getSerialNumber());

        // then
        assertThat(result.valid()).isFalse();
        assertThat(result.status()).isEqualTo("EXPIRED");
        assertThat(result.recipientName()).isEqualTo("김*엠");
        assertThat(result.type()).isEqualTo(CertificateType.COMPLETION);
        assertThat(result.issuer()).isEqualTo(CertificateIssuer.UNIVERSITY_MAKEUS_CHALLENGE);
        assertThat(result.gisuGeneration()).isEqualTo(7L);
    }

    @Test
    @DisplayName("공개 검증 결과의 수신자 이름이 비어 있으면 별표 한 글자로 마스킹한다")
    void 공개_검증_결과의_수신자_이름이_비어_있으면_별표_한_글자로_마스킹한다() {
        // given
        Certificate certificate = Certificate.issue(CertificateIssueSpec.builder()
            .serialNumber("UMC-CMP-20260701-ABCDEFGH")
            .type(CertificateType.COMPLETION)
            .issuer(CertificateIssuer.UNIVERSITY_MAKEUS_CHALLENGE)
            .recipientMemberId(1L)
            .recipientName(" ")
            .recipientSchoolName("유엠씨대학교")
            .gisuId(7L)
            .gisuGeneration(7L)
            .issuedByMemberId(10L)
            .issuedAt(NOW)
            .fileId("file-id")
            .fileSha256("b".repeat(64))
            .build());
        given(loadCertificatePort.findBySerialNumber(certificate.getSerialNumber()))
            .willReturn(Optional.of(certificate));
        CertificateQueryService sut = new CertificateQueryService(
            loadCertificatePort,
            getFileUseCase,
            Clock.fixed(NOW, ZoneOffset.UTC)
        );

        // when
        CertificateVerificationInfo result = sut.verifyBySerialNumber(certificate.getSerialNumber());

        // then
        assertThat(result.recipientName()).isEqualTo("*");
    }

    @Test
    @DisplayName("내 인증서 목록은 최신 발급 순서로 변환한다")
    void 내_인증서_목록은_최신_발급_순서로_변환한다() {
        // given
        given(loadCertificatePort.listByRecipientMemberId(1L)).willReturn(List.of(
            Certificate.issue(CertificateIssueSpec.builder()
                .serialNumber("UMC-MRT-20260701-ABCDEFGH")
                .type(CertificateType.MERIT)
                .issuer(CertificateIssuer.NEORDINARY)
                .recipientMemberId(1L)
                .recipientName("김유엠")
                .recipientSchoolName("유엠씨대학교")
                .gisuId(7L)
                .gisuGeneration(7L)
                .meritTitle("대상")
                .issuedByMemberId(10L)
                .issuedAt(NOW)
                .fileId("file-id")
                .fileSha256("c".repeat(64))
                .build())
        ));
        CertificateQueryService sut = new CertificateQueryService(
            loadCertificatePort,
            getFileUseCase,
            Clock.fixed(NOW, ZoneOffset.UTC)
        );

        // when & then
        assertThat(sut.listByMemberId(1L)).hasSize(1);
    }
}
