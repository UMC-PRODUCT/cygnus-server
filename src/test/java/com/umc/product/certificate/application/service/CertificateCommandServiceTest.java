package com.umc.product.certificate.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionOperations;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.certificate.application.port.in.command.dto.AdminIssueCertificateCommand;
import com.umc.product.certificate.application.port.in.command.dto.CertificateIssueInfo;
import com.umc.product.certificate.application.port.in.command.dto.IssueCertificateCommand;
import com.umc.product.certificate.application.port.out.LoadCertificatePort;
import com.umc.product.certificate.application.port.out.LockCertificateIssuancePort;
import com.umc.product.certificate.application.port.out.RenderCertificatePdfPort;
import com.umc.product.certificate.application.port.out.SaveCertificatePort;
import com.umc.product.certificate.application.port.out.dto.CertificatePdfRenderCommand;
import com.umc.product.certificate.domain.Certificate;
import com.umc.product.certificate.domain.CertificateIssueSpec;
import com.umc.product.certificate.domain.CertificateIssuer;
import com.umc.product.certificate.domain.CertificateTemplate;
import com.umc.product.global.logging.OperationalMetrics;
import com.umc.product.storage.application.port.in.command.ManageFileUsageUseCase;
import com.umc.product.storage.application.port.in.command.ManageFileUseCase;
import com.umc.product.storage.application.port.in.command.StoreGeneratedFileUseCase;
import com.umc.product.storage.application.port.in.command.dto.DeleteFileCommand;
import com.umc.product.storage.application.port.in.command.dto.GeneratedFileInfo;
import com.umc.product.storage.application.port.in.command.dto.ReplaceFileUsagesCommand;
import com.umc.product.storage.domain.FileUsageCoordinate;
import com.umc.product.storage.domain.exception.StorageErrorCode;
import com.umc.product.storage.domain.exception.StorageException;

@ExtendWith(MockitoExtension.class)
class CertificateCommandServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-01T00:00:00Z");

    @Mock
    LoadCertificatePort loadCertificatePort;

    @Mock
    LockCertificateIssuancePort lockCertificateIssuancePort;

    @Mock
    SaveCertificatePort saveCertificatePort;

    @Mock
    StoreGeneratedFileUseCase storeGeneratedFileUseCase;

    @Mock
    ManageFileUseCase manageFileUseCase;

    @Mock
    ManageFileUsageUseCase manageFileUsageUseCase;

    @Mock
    RenderCertificatePdfPort renderCertificatePdfPort;

    @Mock
    CertificateSerialNumberGenerator serialNumberGenerator;

    @Mock
    CertificateIssueContextResolver contextResolver;

    @Mock
    GetChallengerRoleUseCase getChallengerRoleUseCase;

    @Mock
    OperationalMetrics operationalMetrics;

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
        givenCertificateSaveAssignsId(101L);
        CertificateCommandService sut = sut();
        ArgumentCaptor<Certificate> certificateCaptor = ArgumentCaptor.forClass(Certificate.class);
        ArgumentCaptor<CertificatePdfRenderCommand> renderCommandCaptor =
            ArgumentCaptor.forClass(CertificatePdfRenderCommand.class);
        ArgumentCaptor<ReplaceFileUsagesCommand> usageCaptor =
            ArgumentCaptor.forClass(ReplaceFileUsagesCommand.class);

        // when
        CertificateIssueInfo result = sut.issue(command);

        // then
        verify(saveCertificatePort).save(certificateCaptor.capture());
        verify(renderCertificatePdfPort).render(renderCommandCaptor.capture());
        verify(manageFileUsageUseCase).replaceUsages(usageCaptor.capture());
        Certificate saved = certificateCaptor.getValue();
        assertThat(result.serialNumber()).isEqualTo("UMC-CMP-20260701-ABCDEFGH");
        assertThat(renderCommandCaptor.getValue().template().issuer())
            .isEqualTo(CertificateIssuer.UNIVERSITY_MAKEUS_CHALLENGE);
        assertThat(saved.getTemplate()).isEqualTo(CertificateTemplate.UMC_COURSE_COMPLETION);
        assertThat(saved.getTemplate().issuer()).isEqualTo(CertificateIssuer.UNIVERSITY_MAKEUS_CHALLENGE);
        assertThat(saved.getFileId()).isEqualTo("file-id");
        assertThat(saved.getFileSha256()).isEqualTo(sha256(pdfBytes));
        assertThat(usageCaptor.getValue().owner())
            .isEqualTo(FileUsageCoordinate.of("certificate", "101", "file"));
        assertThat(usageCaptor.getValue().fileIds()).containsExactly("file-id");
        assertThat(usageCaptor.getValue().requesterMemberId()).isEqualTo(1L);
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
        givenCertificateSaveAssignsId(102L);
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

    @Test
    @DisplayName("재발급 PDF 생성이 실패하면 기존 인증서를 폐기하지 않는다")
    void 재발급_PDF_생성이_실패하면_기존_인증서를_폐기하지_않는다() {
        // given
        AdminIssueCertificateCommand command = AdminIssueCertificateCommand.builder()
            .template(CertificateTemplate.UMC_DEMO_DAY_FIRST_PRIZE)
            .requesterMemberId(99L)
            .recipientMemberId(1L)
            .gisuId(7L)
            .reissue(true)
            .build();
        Certificate existing = certificate("UMC-MRT-20260601-EXISTING");
        given(getChallengerRoleUseCase.isSuperAdmin(99L)).willReturn(true);
        given(contextResolver.resolveAdmin(command)).willReturn(meritTemplateContext());
        given(loadCertificatePort.findValidByScope(
            CertificateTemplate.UMC_DEMO_DAY_FIRST_PRIZE,
            1L,
            7L,
            "최우수상",
            NOW
        )).willReturn(Optional.of(existing));
        given(serialNumberGenerator.generate(CertificateTemplate.UMC_DEMO_DAY_FIRST_PRIZE, NOW))
            .willReturn("UMC-MRT-20260701-ABCDEFGH");
        given(loadCertificatePort.existsBySerialNumber("UMC-MRT-20260701-ABCDEFGH")).willReturn(false);
        given(renderCertificatePdfPort.render(org.mockito.ArgumentMatchers.any()))
            .willThrow(new IllegalStateException("render failed"));
        CertificateCommandService sut = sut();

        // when & then
        assertThatThrownBy(() -> sut.issueByAdmin(command))
            .isInstanceOf(IllegalStateException.class);
        assertThat(existing.getStatus()).isEqualTo(com.umc.product.certificate.domain.CertificateStatus.ISSUED);
        verify(saveCertificatePort, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("재발급 파일 저장이 실패하면 기존 인증서를 폐기하지 않는다")
    void 재발급_파일_저장이_실패하면_기존_인증서를_폐기하지_않는다() {
        // given
        AdminIssueCertificateCommand command = AdminIssueCertificateCommand.builder()
            .template(CertificateTemplate.UMC_DEMO_DAY_FIRST_PRIZE)
            .requesterMemberId(99L)
            .recipientMemberId(1L)
            .gisuId(7L)
            .reissue(true)
            .build();
        Certificate existing = certificate("UMC-MRT-20260601-EXISTING");
        byte[] pdfBytes = "pdf-content".getBytes(StandardCharsets.UTF_8);
        given(getChallengerRoleUseCase.isSuperAdmin(99L)).willReturn(true);
        given(contextResolver.resolveAdmin(command)).willReturn(meritTemplateContext());
        given(loadCertificatePort.findValidByScope(
            CertificateTemplate.UMC_DEMO_DAY_FIRST_PRIZE,
            1L,
            7L,
            "최우수상",
            NOW
        )).willReturn(Optional.of(existing));
        given(serialNumberGenerator.generate(CertificateTemplate.UMC_DEMO_DAY_FIRST_PRIZE, NOW))
            .willReturn("UMC-MRT-20260701-ABCDEFGH");
        given(loadCertificatePort.existsBySerialNumber("UMC-MRT-20260701-ABCDEFGH")).willReturn(false);
        given(renderCertificatePdfPort.render(org.mockito.ArgumentMatchers.any())).willReturn(pdfBytes);
        given(storeGeneratedFileUseCase.store(org.mockito.ArgumentMatchers.any()))
            .willThrow(new IllegalStateException("storage failed"));
        CertificateCommandService sut = sut();

        // when & then
        assertThatThrownBy(() -> sut.issueByAdmin(command))
            .isInstanceOf(IllegalStateException.class);
        assertThat(existing.getStatus()).isEqualTo(com.umc.product.certificate.domain.CertificateStatus.ISSUED);
        verify(saveCertificatePort, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("재발급은 기존 인증서 usage를 제거하지 않고 새 인증서 usage를 추가한다")
    void 재발급은_기존_인증서_usage를_유지하고_새_인증서_usage를_추가한다() {
        // given
        AdminIssueCertificateCommand command = AdminIssueCertificateCommand.builder()
            .template(CertificateTemplate.UMC_DEMO_DAY_FIRST_PRIZE)
            .requesterMemberId(99L)
            .recipientMemberId(1L)
            .gisuId(7L)
            .reissue(true)
            .build();
        Certificate existing = certificate("UMC-MRT-20260601-EXISTING");
        ReflectionTestUtils.setField(existing, "id", 201L);
        byte[] pdfBytes = "pdf-content".getBytes(StandardCharsets.UTF_8);
        given(getChallengerRoleUseCase.isSuperAdmin(99L)).willReturn(true);
        given(contextResolver.resolveAdmin(command)).willReturn(meritTemplateContext());
        given(loadCertificatePort.findValidByScope(
            CertificateTemplate.UMC_DEMO_DAY_FIRST_PRIZE,
            1L,
            7L,
            "최우수상",
            NOW
        )).willReturn(Optional.of(existing));
        given(serialNumberGenerator.generate(CertificateTemplate.UMC_DEMO_DAY_FIRST_PRIZE, NOW))
            .willReturn("UMC-MRT-20260701-ABCDEFGH");
        given(loadCertificatePort.existsBySerialNumber("UMC-MRT-20260701-ABCDEFGH")).willReturn(false);
        given(renderCertificatePdfPort.render(org.mockito.ArgumentMatchers.any())).willReturn(pdfBytes);
        given(storeGeneratedFileUseCase.store(org.mockito.ArgumentMatchers.any()))
            .willReturn(GeneratedFileInfo.of("new-file-id", "private/certificate/new-file.pdf", pdfBytes.length));
        givenCertificateSaveAssignsId(202L);
        CertificateCommandService sut = sut();
        ArgumentCaptor<ReplaceFileUsagesCommand> usageCaptor =
            ArgumentCaptor.forClass(ReplaceFileUsagesCommand.class);

        // when
        sut.issueByAdmin(command);

        // then
        assertThat(existing.getStatus()).isEqualTo(com.umc.product.certificate.domain.CertificateStatus.REVOKED);
        verify(manageFileUsageUseCase).replaceUsages(usageCaptor.capture());
        assertThat(usageCaptor.getValue().owner())
            .isEqualTo(FileUsageCoordinate.of("certificate", "202", "file"));
        assertThat(usageCaptor.getValue().fileIds()).containsExactly("new-file-id");
        verify(manageFileUsageUseCase, never()).removeAll(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("인증서 usage 저장 실패는 발급을 실패시키고 생성 파일을 tokenized 삭제 경로로 보상한다")
    void 인증서_usage_저장_실패는_발급을_실패시키고_생성_파일을_보상한다() {
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
        givenCertificateSaveAssignsId(301L);
        willThrow(new IllegalStateException("usage failed"))
            .given(manageFileUsageUseCase)
            .replaceUsages(org.mockito.ArgumentMatchers.any());
        CertificateCommandService sut = sut();

        // when & then
        assertThatThrownBy(() -> sut.issue(command))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("usage failed");
        verify(manageFileUsageUseCase).replaceUsages(new ReplaceFileUsagesCommand(
            FileUsageCoordinate.of("certificate", "301", "file"),
            Set.of("file-id"),
            1L
        ));
        verify(manageFileUseCase).deleteFile(new DeleteFileCommand("file-id", 1L));
        verify(operationalMetrics).recordBatchJob(
            "certificate-generated-file-compensation",
            "deleted",
            Duration.ZERO,
            1L
        );
    }

    @Test
    @DisplayName("동시 발급으로 먼저 저장된 인증서가 있으면 잠금 후 기존 인증서를 반환한다")
    void 동시_발급으로_먼저_저장된_인증서가_있으면_잠금_후_기존_인증서를_반환한다() {
        // given
        IssueCertificateCommand command = IssueCertificateCommand.builder()
            .template(CertificateTemplate.UMC_COURSE_COMPLETION)
            .requesterMemberId(1L)
            .gisuId(7L)
            .build();
        Certificate concurrent = certificate("UMC-CMP-20260701-CONCURNT");
        byte[] pdfBytes = "pdf-content".getBytes(StandardCharsets.UTF_8);
        given(contextResolver.resolveSelf(command)).willReturn(completionContext());
        given(loadCertificatePort.findValidByScope(
            CertificateTemplate.UMC_COURSE_COMPLETION,
            1L,
            7L,
            null,
            NOW
        )).willReturn(Optional.empty(), Optional.of(concurrent));
        given(serialNumberGenerator.generate(CertificateTemplate.UMC_COURSE_COMPLETION, NOW))
            .willReturn("UMC-CMP-20260701-ABCDEFGH");
        given(loadCertificatePort.existsBySerialNumber("UMC-CMP-20260701-ABCDEFGH")).willReturn(false);
        given(renderCertificatePdfPort.render(org.mockito.ArgumentMatchers.any())).willReturn(pdfBytes);
        given(storeGeneratedFileUseCase.store(org.mockito.ArgumentMatchers.any()))
            .willReturn(GeneratedFileInfo.of("file-id", "private/certificate/file.pdf", pdfBytes.length));
        CertificateCommandService sut = sut();

        // when
        CertificateIssueInfo result = sut.issue(command);

        // then
        assertThat(result.serialNumber()).isEqualTo(concurrent.getSerialNumber());
        verify(lockCertificateIssuancePort).lockScope(
            CertificateTemplate.UMC_COURSE_COMPLETION,
            1L,
            7L,
            null
        );
        verify(saveCertificatePort, never()).save(org.mockito.ArgumentMatchers.any());
        verify(manageFileUsageUseCase, never()).replaceUsages(org.mockito.ArgumentMatchers.any());
        verify(manageFileUseCase).deleteFile(org.mockito.ArgumentMatchers.any(DeleteFileCommand.class));
        verify(operationalMetrics).recordBatchJob(
            "certificate-generated-file-compensation",
            "deleted",
            Duration.ZERO,
            1L
        );
    }

    @Test
    @DisplayName("registry가 READY 전이면 중복 발급 생성 파일을 물리 삭제하지 않고 deferred metric을 남긴다")
    void registry_READY_전이면_중복_발급_생성_파일을_deferred_orphan으로_남긴다() {
        // given
        IssueCertificateCommand command = IssueCertificateCommand.builder()
            .template(CertificateTemplate.UMC_COURSE_COMPLETION)
            .requesterMemberId(1L)
            .gisuId(7L)
            .build();
        Certificate concurrent = certificate("UMC-CMP-20260701-CONCURNT");
        byte[] pdfBytes = "pdf-content".getBytes(StandardCharsets.UTF_8);
        given(contextResolver.resolveSelf(command)).willReturn(completionContext());
        given(loadCertificatePort.findValidByScope(
            CertificateTemplate.UMC_COURSE_COMPLETION,
            1L,
            7L,
            null,
            NOW
        )).willReturn(Optional.empty(), Optional.of(concurrent));
        given(serialNumberGenerator.generate(CertificateTemplate.UMC_COURSE_COMPLETION, NOW))
            .willReturn("UMC-CMP-20260701-ABCDEFGH");
        given(loadCertificatePort.existsBySerialNumber("UMC-CMP-20260701-ABCDEFGH")).willReturn(false);
        given(renderCertificatePdfPort.render(org.mockito.ArgumentMatchers.any())).willReturn(pdfBytes);
        given(storeGeneratedFileUseCase.store(org.mockito.ArgumentMatchers.any()))
            .willReturn(GeneratedFileInfo.of("file-id", "private/certificate/file.pdf", pdfBytes.length));
        willThrow(new StorageException(StorageErrorCode.FILE_USAGE_REGISTRY_NOT_READY))
            .given(manageFileUseCase)
            .deleteFile(new DeleteFileCommand("file-id", 1L));
        CertificateCommandService sut = sut();

        // when
        CertificateIssueInfo result = sut.issue(command);

        // then
        assertThat(result.serialNumber()).isEqualTo(concurrent.getSerialNumber());
        verify(manageFileUseCase).deleteFile(new DeleteFileCommand("file-id", 1L));
        verify(operationalMetrics).recordBatchJob(
            "certificate-generated-file-compensation",
            "deferred_not_ready",
            Duration.ZERO,
            1L
        );
    }

    @Test
    @DisplayName("신규 인증서 DB 저장이 실패하면 생성 파일을 삭제한다")
    void 신규_인증서_DB_저장이_실패하면_생성_파일을_삭제한다() {
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
            .willThrow(new IllegalStateException("database failed"));
        CertificateCommandService sut = sut();

        // when & then
        assertThatThrownBy(() -> sut.issue(command))
            .isInstanceOf(IllegalStateException.class);
        verify(manageFileUseCase).deleteFile(org.mockito.ArgumentMatchers.any(DeleteFileCommand.class));
    }

    private CertificateCommandService sut() {
        return new CertificateCommandService(
            loadCertificatePort,
            lockCertificateIssuancePort,
            saveCertificatePort,
            storeGeneratedFileUseCase,
            manageFileUseCase,
            manageFileUsageUseCase,
            renderCertificatePdfPort,
            serialNumberGenerator,
            contextResolver,
            getChallengerRoleUseCase,
            new CertificateProperties("/api/v1/certificates/verify/{serialNumber}"),
            TransactionOperations.withoutTransaction(),
            operationalMetrics,
            Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    private void givenCertificateSaveAssignsId(Long certificateId) {
        given(saveCertificatePort.save(org.mockito.ArgumentMatchers.any(Certificate.class)))
            .willAnswer(invocation -> {
                Certificate certificate = invocation.getArgument(0);
                if (certificate.getId() == null) {
                    ReflectionTestUtils.setField(certificate, "id", certificateId);
                }
                return certificate;
            });
    }

    private CertificateIssueContext completionContext() {
        return new CertificateIssueContext(
            CertificateTemplate.UMC_COURSE_COMPLETION,
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
