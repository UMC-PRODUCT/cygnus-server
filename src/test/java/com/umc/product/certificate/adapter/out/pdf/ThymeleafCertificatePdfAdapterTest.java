package com.umc.product.certificate.adapter.out.pdf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.umc.product.certificate.application.port.out.dto.CertificatePdfRenderCommand;
import com.umc.product.certificate.domain.CertificateTemplate;
import com.umc.product.certificate.domain.exception.CertificateException;

class ThymeleafCertificatePdfAdapterTest {

    @Test
    @DisplayName("선택한 인증서 템플릿 배경 위에 발급번호를 포함한 PDF를 렌더링한다")
    void 선택한_인증서_템플릿_배경_위에_발급번호를_포함한_PDF를_렌더링한다() throws Exception {
        // given
        ThymeleafCertificatePdfAdapter sut = new ThymeleafCertificatePdfAdapter();

        // when
        byte[] result = sut.render(CertificatePdfRenderCommand.builder()
            .issuanceNumber("UMC-MRT-20260701-ABCDEFGH")
            .template(CertificateTemplate.UMC_DEMO_DAY_FIRST_PRIZE)
            .recipientName("김유엠")
            .recipientSchoolName("유엠씨대학교")
            .gisuGeneration(7L)
            .meritTitle("커스텀 공로상")
            .meritDescription("커스텀 설명입니다.")
            .issuedAt(Instant.parse("2026-07-01T00:00:00Z"))
            .expiresAt(Instant.parse("2027-07-01T00:00:00Z"))
            .verificationUrl("/api/v1/certificates/verify/UMC-MRT-20260701-ABCDEFGH")
            .build());

        // then
        assertThat(new String(result, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
        assertThat(result).hasSizeGreaterThan(20_000);
        assertThat(extractText(result)).contains("UMC-MRT-20260701-ABCDEFGH");
        writeSample("umc-demo-day-first-prize-custom.pdf", result);
    }

    @ParameterizedTest
    @EnumSource(CertificateTemplate.class)
    @DisplayName("모든 인증서 템플릿은 발급번호를 포함한 PDF로 렌더링된다")
    void 모든_인증서_템플릿은_발급번호를_포함한_PDF로_렌더링된다(CertificateTemplate template) throws Exception {
        // given
        ThymeleafCertificatePdfAdapter sut = new ThymeleafCertificatePdfAdapter();
        String issuanceNumber = "UMC-" + template.serialCode() + "-20260701-ABCDEFGH";

        // when
        byte[] result = sut.render(CertificatePdfRenderCommand.builder()
            .issuanceNumber(issuanceNumber)
            .template(template)
            .recipientName("김유엠")
            .recipientSchoolName("유엠씨대학교")
            .gisuGeneration(7L)
            .meritTitle(template.defaultMeritTitle())
            .issuedAt(Instant.parse("2026-07-01T00:00:00Z"))
            .expiresAt(Instant.parse("2027-07-01T00:00:00Z"))
            .verificationUrl("/api/v1/certificates/verify/" + issuanceNumber)
            .build());

        // then
        assertThat(new String(result, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
        assertThat(extractText(result).replace('\u00A0', ' '))
            .contains(issuanceNumber, template.issuer().displayName());
    }

    @Test
    @DisplayName("인증서 PDF 렌더링은 명시적 템플릿이 필요하다")
    void 인증서_PDF_렌더링은_명시적_템플릿이_필요하다() {
        // given
        ThymeleafCertificatePdfAdapter sut = new ThymeleafCertificatePdfAdapter();

        CertificatePdfRenderCommand command = CertificatePdfRenderCommand.builder()
            .issuanceNumber("UMC-CMP-20260701-ABCDEFGH")
            .recipientName("김유엠")
            .recipientSchoolName("유엠씨대학교")
            .gisuGeneration(7L)
            .issuedAt(Instant.parse("2026-07-01T00:00:00Z"))
            .expiresAt(Instant.parse("2027-07-01T00:00:00Z"))
            .verificationUrl("/api/v1/certificates/verify/UMC-CMP-20260701-ABCDEFGH")
            .build();

        // when & then
        assertThatThrownBy(() -> sut.render(command))
            .isInstanceOf(CertificateException.class);
    }

    @Test
    @DisplayName("레이아웃의 도형·fallback 색상·blank·wrap·clip 분기를 실제 PDF에 반영한다")
    void 레이아웃의_도형_fallback_색상_blank_wrap_clip_분기를_실제_PDF에_반영한다() throws Exception {
        // given
        ThymeleafCertificatePdfAdapter sut =
            new ThymeleafCertificatePdfAdapter("certificate/config/certificate_template_test_edge.json");

        // when
        byte[] result = sut.render(command("https://example.com/verify", "가나다라마바사", "김 유 엠"));

        // then
        assertThat(new String(result, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
        assertThat(extractText(result)).contains("STATIC");
    }

    @Test
    @DisplayName("최소 글꼴로도 필드에 맞지 않고 overflow 정책이 error이면 렌더링을 거부한다")
    void 최소_글꼴로도_필드에_맞지_않고_overflow_정책이_error이면_렌더링을_거부한다() {
        ThymeleafCertificatePdfAdapter sut =
            new ThymeleafCertificatePdfAdapter("certificate/config/certificate_template_test_overflow.json");

        assertThatThrownBy(() -> sut.render(command("https://example.com/verify", "김유엠", "대상")))
            .isInstanceOf(CertificateException.class);
    }

    @Test
    @DisplayName("템플릿 설정 리소스가 없으면 렌더링 예외로 변환한다")
    void 템플릿_설정_리소스가_없으면_렌더링_예외로_변환한다() {
        ThymeleafCertificatePdfAdapter sut = new ThymeleafCertificatePdfAdapter("missing-config.json");

        assertThatThrownBy(() -> sut.render(command("https://example.com/verify", "김유엠", "대상")))
            .isInstanceOf(CertificateException.class);
    }

    @Test
    @DisplayName("QR 코드로 만들 수 없는 빈 검증 URL은 렌더링 예외로 변환한다")
    void QR_코드로_만들_수_없는_빈_검증_URL은_렌더링_예외로_변환한다() {
        ThymeleafCertificatePdfAdapter sut = new ThymeleafCertificatePdfAdapter();

        assertThatThrownBy(() -> sut.render(command("", "김유엠", "대상")))
            .isInstanceOf(CertificateException.class);
    }

    private CertificatePdfRenderCommand command(String verificationUrl, String recipientName, String meritTitle) {
        return CertificatePdfRenderCommand.builder()
            .issuanceNumber("UMC-MRT-20260701-ABCDEFGH")
            .template(CertificateTemplate.UMC_DEMO_DAY_FIRST_PRIZE)
            .recipientName(recipientName)
            .recipientSchoolName("가나다라마바사")
            .gisuGeneration(7L)
            .meritTitle(meritTitle)
            .issuedAt(Instant.parse("2026-07-01T00:00:00Z"))
            .expiresAt(Instant.parse("2027-07-01T00:00:00Z"))
            .verificationUrl(verificationUrl)
            .build();
    }

    private String extractText(byte[] pdfBytes) throws IOException {
        try (PDDocument document = PDDocument.load(pdfBytes)) {
            return new PDFTextStripper().getText(document);
        }
    }

    private void writeSample(String fileName, byte[] pdfBytes) throws IOException {
        Path sampleDir = Path.of("build", "certificate-samples");
        Files.createDirectories(sampleDir);
        Files.write(sampleDir.resolve(fileName), pdfBytes);
    }

}
