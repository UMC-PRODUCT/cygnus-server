package com.umc.product.certificate.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import com.umc.product.certificate.domain.CertificateTemplate;
import com.umc.product.certificate.domain.exception.CertificateException;
import com.umc.product.storage.application.port.in.query.GetFileUseCase;
import com.umc.product.storage.application.port.in.query.dto.FileInfo;
import com.umc.product.storage.domain.enums.FileCategory;

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
            .template(CertificateTemplate.UMC_COURSE_COMPLETION)
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
        assertThat(result.template()).isEqualTo(CertificateTemplate.UMC_COURSE_COMPLETION);
        assertThat(result.issuer()).isEqualTo(CertificateIssuer.UNIVERSITY_MAKEUS_CHALLENGE);
        assertThat(result.gisuGeneration()).isEqualTo(7L);
    }

    @Test
    @DisplayName("공개 검증 결과의 수신자 이름이 비어 있으면 빈 문자열을 반환한다")
    void 공개_검증_결과의_수신자_이름이_비어_있으면_빈_문자열을_반환한다() {
        // given
        Certificate certificate = Certificate.issue(CertificateIssueSpec.builder()
            .serialNumber("UMC-CMP-20260701-ABCDEFGH")
            .template(CertificateTemplate.UMC_COURSE_COMPLETION)
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
        assertThat(result.recipientName()).isEmpty();
    }

    @Test
    @DisplayName("내 인증서 목록은 최신 발급 순서로 변환한다")
    void 내_인증서_목록은_최신_발급_순서로_변환한다() {
        // given
        given(loadCertificatePort.listByRecipientMemberId(1L)).willReturn(List.of(
            Certificate.issue(CertificateIssueSpec.builder()
                .serialNumber("UMC-MRT-20260701-ABCDEFGH")
                .template(CertificateTemplate.NEORDINARY_HACKATHON_GRAND_PRIZE)
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

    @Test
    @DisplayName("유효한 본인 인증서는 파일 다운로드 정보를 반환한다")
    void 유효한_본인_인증서는_파일_다운로드_정보를_반환한다() {
        // given
        Certificate certificate = certificate("김유엠", NOW);
        given(loadCertificatePort.getById(10L)).willReturn(certificate);
        given(getFileUseCase.getById("file-id")).willReturn(new FileInfo(
            "file-id",
            "certificate.pdf",
            FileCategory.CERTIFICATE,
            "application/pdf",
            100L,
            "https://cdn.example.com/certificate.pdf",
            true,
            1L,
            NOW
        ));
        CertificateQueryService sut = sut();

        // when
        var result = sut.getDownloadInfo(10L, 1L);

        // then
        assertThat(result.serialNumber()).isEqualTo(certificate.getSerialNumber());
        assertThat(result.downloadUrl()).isEqualTo("https://cdn.example.com/certificate.pdf");
    }

    @Test
    @DisplayName("다른 회원의 인증서는 다운로드할 수 없다")
    void 다른_회원의_인증서는_다운로드할_수_없다() {
        given(loadCertificatePort.getById(10L)).willReturn(certificate("김유엠", NOW));

        assertThatThrownBy(() -> sut().getDownloadInfo(10L, 2L))
            .isInstanceOf(CertificateException.class);
    }

    @Test
    @DisplayName("만료된 인증서는 소유자도 다운로드할 수 없다")
    void 만료된_인증서는_소유자도_다운로드할_수_없다() {
        given(loadCertificatePort.getById(10L))
            .willReturn(certificate("김유엠", NOW.minus(366, ChronoUnit.DAYS)));

        assertThatThrownBy(() -> sut().getDownloadInfo(10L, 1L))
            .isInstanceOf(CertificateException.class);
    }

    @Test
    @DisplayName("한 글자와 두 글자 이름은 노출 없이 길이에 맞게 마스킹한다")
    void 한_글자와_두_글자_이름은_노출_없이_길이에_맞게_마스킹한다() {
        Certificate oneCharacter = certificate("김", NOW);
        Certificate twoCharacters = certificate("김유", NOW);
        given(loadCertificatePort.findBySerialNumber("one")).willReturn(Optional.of(oneCharacter));
        given(loadCertificatePort.findBySerialNumber("two")).willReturn(Optional.of(twoCharacters));
        CertificateQueryService sut = sut();

        assertThat(sut.verifyBySerialNumber("one").recipientName()).isEqualTo("*");
        assertThat(sut.verifyBySerialNumber("two").recipientName()).isEqualTo("김*");
    }

    private CertificateQueryService sut() {
        return new CertificateQueryService(
            loadCertificatePort,
            getFileUseCase,
            Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    private Certificate certificate(String recipientName, Instant issuedAt) {
        return Certificate.issue(CertificateIssueSpec.builder()
            .serialNumber("UMC-CMP-20260701-QUERY001")
            .template(CertificateTemplate.UMC_COURSE_COMPLETION)
            .recipientMemberId(1L)
            .recipientName(recipientName)
            .recipientSchoolName("유엠씨대학교")
            .gisuId(7L)
            .gisuGeneration(7L)
            .issuedByMemberId(10L)
            .issuedAt(issuedAt)
            .fileId("file-id")
            .fileSha256("b".repeat(64))
            .build());
    }
}
