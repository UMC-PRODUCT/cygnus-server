package com.umc.product.certificate.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.certificate.application.port.in.query.dto.CertificatePdfPreviewQuery;
import com.umc.product.certificate.application.port.out.RenderCertificatePdfPort;
import com.umc.product.certificate.application.port.out.dto.CertificatePdfRenderCommand;
import com.umc.product.certificate.domain.CertificateTemplate;

@ExtendWith(MockitoExtension.class)
class CertificatePdfPreviewServiceTest {

    @Mock
    RenderCertificatePdfPort renderCertificatePdfPort;

    @Test
    @DisplayName("path-only 검증 URL은 요청 origin과 합쳐 PDF 렌더링에 전달한다")
    void path_only_검증_URL은_요청_origin과_합쳐_PDF_렌더링에_전달한다() {
        // given
        CertificatePdfPreviewService sut = new CertificatePdfPreviewService(
            renderCertificatePdfPort,
            new CertificateProperties("/api/v1/certificates/verify/{serialNumber}")
        );
        byte[] pdfBytes = "%PDF".getBytes();
        ArgumentCaptor<CertificatePdfRenderCommand> captor = ArgumentCaptor.forClass(CertificatePdfRenderCommand.class);
        given(renderCertificatePdfPort.render(captor.capture())).willReturn(pdfBytes);

        // when
        var result = sut.preview(CertificatePdfPreviewQuery.builder()
            .template(CertificateTemplate.UMC_DEMO_DAY_FIRST_PRIZE)
            .issuanceNumber("UMC-MRT-20260703-SAMPLE01")
            .recipientName("김유엠")
            .recipientSchoolName("유엠씨대학교")
            .gisuGeneration(7L)
            .requestOrigin("http://localhost:8080")
            .build());

        // then
        assertThat(result.content()).isEqualTo(pdfBytes);
        assertThat(result.fileName()).isEqualTo("umc_demo_day_first_prize-UMC-MRT-20260703-SAMPLE01.pdf");
        assertThat(captor.getValue().verificationUrl())
            .isEqualTo("http://localhost:8080/api/v1/certificates/verify/UMC-MRT-20260703-SAMPLE01");
    }

    @Test
    @DisplayName("명시한 검증 URL은 origin 보정 없이 그대로 PDF 렌더링에 전달한다")
    void 명시한_검증_URL은_origin_보정_없이_그대로_PDF_렌더링에_전달한다() {
        // given
        CertificatePdfPreviewService sut = new CertificatePdfPreviewService(
            renderCertificatePdfPort,
            new CertificateProperties("/api/v1/certificates/verify/{serialNumber}")
        );
        ArgumentCaptor<CertificatePdfRenderCommand> captor = ArgumentCaptor.forClass(CertificatePdfRenderCommand.class);
        given(renderCertificatePdfPort.render(captor.capture())).willReturn("%PDF".getBytes());

        // when
        sut.preview(CertificatePdfPreviewQuery.builder()
            .template(CertificateTemplate.NEORDINARY_HACKATHON_GRAND_PRIZE)
            .issuanceNumber("UMC-MRT-20260703-SAMPLE01")
            .recipientName("김유엠")
            .recipientSchoolName("유엠씨대학교")
            .gisuGeneration(7L)
            .verificationUrl("https://api.example.com/certificates/verify/sample")
            .requestOrigin("http://localhost:8080")
            .build());

        // then
        assertThat(captor.getValue().verificationUrl()).isEqualTo("https://api.example.com/certificates/verify/sample");
        verify(renderCertificatePdfPort).render(captor.getValue());
    }
}
