package com.umc.product.certificate.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionOperations;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.certificate.application.port.in.command.dto.AdminIssueCertificateCommand;
import com.umc.product.certificate.application.port.in.command.dto.CertificateIssueInfo;
import com.umc.product.certificate.application.port.in.command.dto.IssueCertificateCommand;
import com.umc.product.certificate.application.port.out.LoadCertificatePort;
import com.umc.product.certificate.application.port.out.RenderCertificatePdfPort;
import com.umc.product.certificate.application.port.out.SaveCertificatePort;
import com.umc.product.certificate.application.port.out.dto.CertificatePdfRenderCommand;
import com.umc.product.certificate.domain.Certificate;
import com.umc.product.certificate.domain.CertificateIssueSpec;
import com.umc.product.certificate.domain.CertificateIssuer;
import com.umc.product.certificate.domain.CertificateTemplate;
import com.umc.product.storage.application.port.in.command.StoreGeneratedFileUseCase;
import com.umc.product.storage.application.port.in.command.dto.GeneratedFileInfo;

@ExtendWith(MockitoExtension.class)
class CertificateCommandServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-01T00:00:00Z");

    @Mock
    LoadCertificatePort loadCertificatePort;

    @Mock
    SaveCertificatePort saveCertificatePort;

    @Mock
    StoreGeneratedFileUseCase storeGeneratedFileUseCase;

    @Mock
    RenderCertificatePdfPort renderCertificatePdfPort;

    @Mock
    CertificateSerialNumberGenerator serialNumberGenerator;

    @Mock
    CertificateIssueContextResolver contextResolver;

    @Mock
    GetChallengerRoleUseCase getChallengerRoleUseCase;

    @Test
    @DisplayName("기존 유효 인증서가 있으면 새 PDF를 만들지 않고 기존 인증서를 반환한다")
    void 기존_유효_인증서가_있으면_새_PDF를_만들지_않고_기존_인증서를_반환한다() {
        // given
        IssueCertificateCommand command = IssueCertificateCommand.builder()
            .template(CertificateTemplate.UMC_COURSE_COMPLETION)
            .requesterMemberId(1L)
            .gisuId(7L)
            .build();
        CertificateIssueContext context = completionContext();
        Certificate existing = certificate("UMC-CMP-20260701-ABCDEFGH");
        given(contextResolver.resolveSelf(command)).willReturn(context);
        given(loadCertificatePort.findValidByScope(
            CertificateTemplate.UMC_COURSE_COMPLETION,
            1L,
            7L,
            null,
            NOW
        )).willReturn(Optional.of(existing));
        CertificateCommandService sut = sut();

        // when
        CertificateIssueInfo result = sut.issue(command);

        // then
        assertThat(result.serialNumber()).isEqualTo(existing.getSerialNumber());
        verify(renderCertificatePdfPort, never()).render(org.mockito.ArgumentMatchers.any());
        verify(storeGeneratedFileUseCase, never()).store(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("신규 발급 시 PDF SHA-256을 저장하고 생성 파일로 업로드한다")
    void 신규_발급_시_PDF_SHA_256을_저장하고_생성_파일로_업로드한다() throws Exception {
        // given
        IssueCertificateCommand command = IssueCertificateCommand.builder()
            .template(CertificateTemplate.UMC_COURSE_COMPLETION)
            .requesterMemberId(1L)
            .gisuId(7L)
            .build();
        byte[] pdfBytes = "pdf-content".getBytes(StandardCharsets.UTF_8);
        given(contextResolver.resolveSelf(command)).willReturn(completionContext());
        given(loadCertificatePort.findValidByScope(
            CertificateTemplate.UMC_COURSE_COMPLETION,
            1L,
            7L,
            null,
            NOW
        )).willReturn(Optional.empty());
        given(serialNumberGenerator.generate(CertificateTemplate.UMC_COURSE_COMPLETION, NOW))
            .willReturn("UMC-CMP-20260701-ABCDEFGH");
        given(loadCertificatePort.existsBySerialNumber("UMC-CMP-20260701-ABCDEFGH")).willReturn(false);
        given(renderCertificatePdfPort.render(org.mockito.ArgumentMatchers.any())).willReturn(pdfBytes);
        given(storeGeneratedFileUseCase.store(org.mockito.ArgumentMatchers.any()))
            .willReturn(GeneratedFileInfo.of("file-id", "private/certificate/file.pdf", pdfBytes.length));
        given(saveCertificatePort.save(org.mockito.ArgumentMatchers.any(Certificate.class)))
            .willAnswer(invocation -> invocation.getArgument(0));
        CertificateCommandService sut = sut();
        ArgumentCaptor<Certificate> certificateCaptor = ArgumentCaptor.forClass(Certificate.class);
        ArgumentCaptor<CertificatePdfRenderCommand> renderCommandCaptor =
            ArgumentCaptor.forClass(CertificatePdfRenderCommand.class);

        // when
        CertificateIssueInfo result = sut.issue(command);

        // then
        verify(saveCertificatePort).save(certificateCaptor.capture());
        verify(renderCertificatePdfPort).render(renderCommandCaptor.capture());
        Certificate saved = certificateCaptor.getValue();
        assertThat(result.serialNumber()).isEqualTo("UMC-CMP-20260701-ABCDEFGH");
        assertThat(renderCommandCaptor.getValue().issuer()).isEqualTo(CertificateIssuer.UNIVERSITY_MAKEUS_CHALLENGE);
        assertThat(renderCommandCaptor.getValue().template()).isEqualTo(CertificateTemplate.UMC_COURSE_COMPLETION);
        assertThat(saved.getTemplate()).isEqualTo(CertificateTemplate.UMC_COURSE_COMPLETION);
        assertThat(saved.getIssuer()).isEqualTo(CertificateIssuer.UNIVERSITY_MAKEUS_CHALLENGE);
        assertThat(saved.getFileId()).isEqualTo("file-id");
        assertThat(saved.getFileSha256()).isEqualTo(sha256(pdfBytes));
    }

    @Test
    @DisplayName("템플릿 발급 시 선택된 템플릿과 발급번호를 PDF 렌더러로 전달한다")
    void 템플릿_발급_시_선택된_템플릿과_발급번호를_PDF_렌더러로_전달한다() {
        // given
        AdminIssueCertificateCommand command = AdminIssueCertificateCommand.builder()
            .template(CertificateTemplate.UMC_DEMO_DAY_FIRST_PRIZE)
            .requesterMemberId(99L)
            .recipientMemberId(1L)
            .gisuId(7L)
            .build();
        byte[] pdfBytes = "pdf-content".getBytes(StandardCharsets.UTF_8);
        given(getChallengerRoleUseCase.isSuperAdmin(99L)).willReturn(true);
        given(contextResolver.resolveAdmin(command)).willReturn(meritTemplateContext());
        given(loadCertificatePort.findValidByScope(
            CertificateTemplate.UMC_DEMO_DAY_FIRST_PRIZE,
            1L,
            7L,
            "최우수상",
            NOW
        )).willReturn(Optional.empty());
        given(serialNumberGenerator.generate(CertificateTemplate.UMC_DEMO_DAY_FIRST_PRIZE, NOW))
            .willReturn("UMC-MRT-20260701-ABCDEFGH");
        given(loadCertificatePort.existsBySerialNumber("UMC-MRT-20260701-ABCDEFGH")).willReturn(false);
        given(renderCertificatePdfPort.render(org.mockito.ArgumentMatchers.any())).willReturn(pdfBytes);
        given(storeGeneratedFileUseCase.store(org.mockito.ArgumentMatchers.any()))
            .willReturn(GeneratedFileInfo.of("file-id", "private/certificate/file.pdf", pdfBytes.length));
        given(saveCertificatePort.save(org.mockito.ArgumentMatchers.any(Certificate.class)))
            .willAnswer(invocation -> invocation.getArgument(0));
        CertificateCommandService sut = sut();
        ArgumentCaptor<CertificatePdfRenderCommand> renderCommandCaptor =
            ArgumentCaptor.forClass(CertificatePdfRenderCommand.class);

        // when
        sut.issueByAdmin(command);

        // then
        verify(renderCertificatePdfPort).render(renderCommandCaptor.capture());
        CertificatePdfRenderCommand renderCommand = renderCommandCaptor.getValue();
        assertThat(renderCommand.template()).isEqualTo(CertificateTemplate.UMC_DEMO_DAY_FIRST_PRIZE);
        assertThat(renderCommand.issuanceNumber()).isEqualTo("UMC-MRT-20260701-ABCDEFGH");
    }

    private CertificateCommandService sut() {
        return new CertificateCommandService(
            loadCertificatePort,
            saveCertificatePort,
            storeGeneratedFileUseCase,
            renderCertificatePdfPort,
            serialNumberGenerator,
            contextResolver,
            getChallengerRoleUseCase,
            new CertificateProperties("/api/v1/certificates/verify/{serialNumber}"),
            TransactionOperations.withoutTransaction(),
            Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    private CertificateIssueContext completionContext() {
        return new CertificateIssueContext(
            CertificateTemplate.UMC_COURSE_COMPLETION,
            CertificateIssuer.UNIVERSITY_MAKEUS_CHALLENGE,
            1L,
            "김유엠",
            "유엠씨대학교",
            7L,
            7L,
            null,
            null,
            1L
        );
    }

    private CertificateIssueContext meritTemplateContext() {
        return new CertificateIssueContext(
            CertificateTemplate.UMC_DEMO_DAY_FIRST_PRIZE,
            CertificateIssuer.UNIVERSITY_MAKEUS_CHALLENGE,
            1L,
            "김유엠",
            "유엠씨대학교",
            7L,
            7L,
            "최우수상",
            "탁월한 기여",
            99L
        );
    }

    private Certificate certificate(String serialNumber) {
        return Certificate.issue(CertificateIssueSpec.builder()
            .serialNumber(serialNumber)
            .template(CertificateTemplate.UMC_COURSE_COMPLETION)
            .issuer(CertificateIssuer.UNIVERSITY_MAKEUS_CHALLENGE)
            .recipientMemberId(1L)
            .recipientName("김유엠")
            .recipientSchoolName("유엠씨대학교")
            .gisuId(7L)
            .gisuGeneration(7L)
            .issuedByMemberId(1L)
            .issuedAt(NOW)
            .fileId("file-id")
            .fileSha256("a".repeat(64))
            .build());
    }

    private String sha256(byte[] content) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
    }
}
