package com.umc.product.certificate.adapter.out.pdf;

import static org.assertj.core.api.Assertions.assertThat;

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
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import com.umc.product.certificate.application.port.out.dto.CertificatePdfRenderCommand;
import com.umc.product.certificate.domain.CertificateIssuer;
import com.umc.product.certificate.domain.CertificateTemplate;
import com.umc.product.certificate.domain.CertificateType;

class ThymeleafCertificatePdfAdapterTest {

    @Test
    @DisplayName("Thymeleaf 인증서 템플릿을 PDF 바이트로 렌더링한다")
    void Thymeleaf_인증서_템플릿을_PDF_바이트로_렌더링한다() {
        // given
        ThymeleafCertificatePdfAdapter sut = new ThymeleafCertificatePdfAdapter(templateEngine());

        // when
        byte[] result = sut.render(CertificatePdfRenderCommand.builder()
            .serialNumber("UMC-CMP-20260701-ABCDEFGH")
            .type(CertificateType.COMPLETION)
            .issuer(CertificateIssuer.UNIVERSITY_MAKEUS_CHALLENGE)
            .recipientName("김유엠")
            .recipientSchoolName("유엠씨대학교")
            .gisuGeneration(7L)
            .issuedAt(Instant.parse("2026-07-01T00:00:00Z"))
            .expiresAt(Instant.parse("2027-07-01T00:00:00Z"))
            .verificationUrl("/api/v1/certificates/verify/UMC-CMP-20260701-ABCDEFGH")
            .build());

        // then
        assertThat(new String(result, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
        assertThat(result).hasSizeGreaterThan(1_000);
    }

    @Test
    @DisplayName("Neordinary 발급 주체의 공로증 템플릿을 PDF 바이트로 렌더링한다")
    void Neordinary_발급_주체의_공로증_템플릿을_PDF_바이트로_렌더링한다() {
        // given
        ThymeleafCertificatePdfAdapter sut = new ThymeleafCertificatePdfAdapter(templateEngine());

        // when
        byte[] result = sut.render(CertificatePdfRenderCommand.builder()
            .serialNumber("UMC-MRT-20260701-ABCDEFGH")
            .type(CertificateType.MERIT)
            .issuer(CertificateIssuer.NEORDINARY)
            .recipientName("김유엠")
            .recipientSchoolName("유엠씨대학교")
            .gisuGeneration(7L)
            .meritTitle("대상")
            .meritDescription("탁월한 기여")
            .issuedAt(Instant.parse("2026-07-01T00:00:00Z"))
            .expiresAt(Instant.parse("2027-07-01T00:00:00Z"))
            .verificationUrl("/api/v1/certificates/verify/UMC-MRT-20260701-ABCDEFGH")
            .build());

        // then
        assertThat(new String(result, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
        assertThat(result).hasSizeGreaterThan(1_000);
    }

    @Test
    @DisplayName("선택한 인증서 템플릿 배경 위에 발급번호를 포함한 PDF를 렌더링한다")
    void 선택한_인증서_템플릿_배경_위에_발급번호를_포함한_PDF를_렌더링한다() throws Exception {
        // given
        ThymeleafCertificatePdfAdapter sut = new ThymeleafCertificatePdfAdapter(templateEngine());

        // when
        byte[] result = sut.render(CertificatePdfRenderCommand.builder()
            .issuanceNumber("UMC-MRT-20260701-ABCDEFGH")
            .type(CertificateType.MERIT)
            .template(CertificateTemplate.UMC_DEMO_DAY_FIRST_PRIZE)
            .issuer(CertificateIssuer.UNIVERSITY_MAKEUS_CHALLENGE)
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
        ThymeleafCertificatePdfAdapter sut = new ThymeleafCertificatePdfAdapter(templateEngine());
        String issuanceNumber = "UMC-" + template.type().serialCode() + "-20260701-ABCDEFGH";

        // when
        byte[] result = sut.render(CertificatePdfRenderCommand.builder()
            .issuanceNumber(issuanceNumber)
            .type(template.type())
            .template(template)
            .issuer(template.issuer())
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
        assertThat(extractText(result)).contains(issuanceNumber);
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

    private SpringTemplateEngine templateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);

        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }
}
