package com.umc.product.test.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.umc.product.certificate.application.port.in.query.PreviewCertificatePdfUseCase;
import com.umc.product.certificate.application.port.in.query.dto.CertificatePdfPreviewInfo;
import com.umc.product.certificate.application.port.in.query.dto.CertificatePdfPreviewQuery;
import com.umc.product.certificate.domain.CertificateTemplate;
import com.umc.product.global.security.JwtTokenProvider;

@WebMvcTest(controllers = CertificatePdfTestController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("local")
class CertificatePdfTestControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    PreviewCertificatePdfUseCase previewCertificatePdfUseCase;

    @MockitoBean
    JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("인증서 PDF 미리보기는 PDF 바이트를 inline 다운로드로 반환한다")
    void 인증서_PDF_미리보기는_PDF_바이트를_inline_다운로드로_반환한다() throws Exception {
        // given
        byte[] pdfBytes = "%PDF".getBytes();
        ArgumentCaptor<CertificatePdfPreviewQuery> captor = ArgumentCaptor.forClass(CertificatePdfPreviewQuery.class);
        given(previewCertificatePdfUseCase.preview(captor.capture()))
            .willReturn(CertificatePdfPreviewInfo.of("sample.pdf", pdfBytes));

        // when & then
        mockMvc.perform(get("/test/certificates/preview")
                .param("template", "UMC_DEMO_DAY_FIRST_PRIZE")
                .param("recipientName", "김유엠")
                .param("gisuGeneration", "7"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "application/pdf"))
            .andExpect(header().string(
                "Content-Disposition",
                "inline; filename=\"=?UTF-8?Q?sample.pdf?=\"; filename*=UTF-8''sample.pdf"
            ))
            .andExpect(result -> assertThat(result.getResponse().getContentAsByteArray()).isEqualTo(pdfBytes));

        assertThat(captor.getValue().template()).isEqualTo(CertificateTemplate.UMC_DEMO_DAY_FIRST_PRIZE);
        assertThat(captor.getValue().recipientName()).isEqualTo("김유엠");
        assertThat(captor.getValue().requestOrigin()).isEqualTo("http://localhost");
    }
}
