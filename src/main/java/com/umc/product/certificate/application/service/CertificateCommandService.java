package com.umc.product.certificate.application.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;
import java.util.function.Supplier;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionOperations;

import com.umc.product.audit.application.port.in.annotation.Audited;
import com.umc.product.audit.domain.AuditAction;
import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.certificate.application.port.in.command.AdminIssueCertificateUseCase;
import com.umc.product.certificate.application.port.in.command.IssueCertificateUseCase;
import com.umc.product.certificate.application.port.in.command.RevokeCertificateUseCase;
import com.umc.product.certificate.application.port.in.command.dto.AdminIssueCertificateCommand;
import com.umc.product.certificate.application.port.in.command.dto.CertificateIssueInfo;
import com.umc.product.certificate.application.port.in.command.dto.IssueCertificateCommand;
import com.umc.product.certificate.application.port.in.command.dto.RevokeCertificateCommand;
import com.umc.product.certificate.application.port.out.LoadCertificatePort;
import com.umc.product.certificate.application.port.out.RenderCertificatePdfPort;
import com.umc.product.certificate.application.port.out.SaveCertificatePort;
import com.umc.product.certificate.application.port.out.dto.CertificatePdfRenderCommand;
import com.umc.product.certificate.domain.Certificate;
import com.umc.product.certificate.domain.CertificateIssueSpec;
import com.umc.product.certificate.domain.exception.CertificateErrorCode;
import com.umc.product.certificate.domain.exception.CertificateException;
import com.umc.product.global.exception.constant.Domain;
import com.umc.product.storage.application.port.in.command.StoreGeneratedFileUseCase;
import com.umc.product.storage.application.port.in.command.dto.GeneratedFileInfo;
import com.umc.product.storage.application.port.in.command.dto.StoreGeneratedFileCommand;
import com.umc.product.storage.domain.enums.FileCategory;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CertificateCommandService implements
    IssueCertificateUseCase,
    AdminIssueCertificateUseCase,
    RevokeCertificateUseCase {

    private static final int SERIAL_GENERATION_RETRY_COUNT = 5;
    private static final String PDF_CONTENT_TYPE = "application/pdf";

    private final LoadCertificatePort loadCertificatePort;
    private final SaveCertificatePort saveCertificatePort;
    private final StoreGeneratedFileUseCase storeGeneratedFileUseCase;
    private final RenderCertificatePdfPort renderCertificatePdfPort;
    private final CertificateSerialNumberGenerator serialNumberGenerator;
    private final CertificateIssueContextResolver contextResolver;
    private final GetChallengerRoleUseCase getChallengerRoleUseCase;
    private final CertificateProperties certificateProperties;
    private final TransactionOperations transactionOperations;
    private final Clock clock;

    @Override
    @Audited(
        domain = Domain.CERTIFICATE,
        action = AuditAction.CREATE,
        targetType = "Certificate",
        targetId = "#result.certificateId()",
        description = "'인증서를 셀프 발급했습니다.'"
    )
    public CertificateIssueInfo issue(IssueCertificateCommand command) {
        CertificateIssueContext context = contextResolver.resolveSelf(command);
        return issue(context, false);
    }

    @Override
    @Audited(
        domain = Domain.CERTIFICATE,
        action = AuditAction.CREATE,
        targetType = "Certificate",
        targetId = "#result.certificateId()",
        description = "'운영진이 인증서를 발급했습니다.'"
    )
    public CertificateIssueInfo issueByAdmin(AdminIssueCertificateCommand command) {
        validateAdmin(command.requesterMemberId(), command.gisuId());
        CertificateIssueContext context = contextResolver.resolveAdmin(command);
        return issue(context, command.reissue());
    }

    @Override
    @Audited(
        domain = Domain.CERTIFICATE,
        action = AuditAction.UPDATE,
        targetType = "Certificate",
        targetId = "#command.certificateId()",
        description = "'인증서를 폐기했습니다.'"
    )
    @Transactional
    public void revoke(RevokeCertificateCommand command) {
        Certificate certificate = loadCertificatePort.getById(command.certificateId());
        validateAdmin(command.requesterMemberId(), certificate.getGisuId());
        certificate.revoke(command.requesterMemberId(), Instant.now(clock), command.reason());
        saveCertificatePort.save(certificate);
    }

    private CertificateIssueInfo issue(CertificateIssueContext context, boolean reissue) {
        Instant now = Instant.now(clock);
        CertificateIssuePreparation preparation = executeInTransaction(() -> prepareIssue(context, reissue, now));
        if (preparation.existingInfo() != null) {
            return preparation.existingInfo();
        }

        String serialNumber = preparation.serialNumber();
        Instant expiresAt = now.plusSeconds(365L * 24 * 60 * 60);
        byte[] pdfBytes = renderPdf(context, serialNumber, now, expiresAt);
        String fileSha256 = sha256(pdfBytes);
        GeneratedFileInfo fileInfo = storeGeneratedFileUseCase.store(StoreGeneratedFileCommand.of(
            serialNumber + ".pdf",
            PDF_CONTENT_TYPE,
            pdfBytes,
            FileCategory.CERTIFICATE,
            context.issuedByMemberId()
        ));

        return executeInTransaction(() -> saveIssuedCertificate(context, serialNumber, now, fileInfo, fileSha256));
    }

    private CertificateIssuePreparation prepareIssue(CertificateIssueContext context, boolean reissue, Instant now) {
        Certificate existing = loadCertificatePort.findValidByScope(
            context.type(),
            context.issuer(),
            context.recipientMemberId(),
            context.gisuId(),
            context.projectId(),
            context.meritTitle(),
            now
        ).orElse(null);

        if (existing != null && !reissue) {
            return CertificateIssuePreparation.existing(CertificateIssueInfo.from(existing));
        }
        if (existing != null) {
            existing.revoke(context.issuedByMemberId(), now, "재발급");
            saveCertificatePort.save(existing);
        }

        String serialNumber = generateUniqueSerialNumber(context, now);
        return CertificateIssuePreparation.newIssue(serialNumber);
    }

    private CertificateIssueInfo saveIssuedCertificate(
        CertificateIssueContext context,
        String serialNumber,
        Instant issuedAt,
        GeneratedFileInfo fileInfo,
        String fileSha256
    ) {
        Certificate certificate = Certificate.issue(CertificateIssueSpec.builder()
            .serialNumber(serialNumber)
            .type(context.type())
            .issuer(context.issuer())
            .recipientMemberId(context.recipientMemberId())
            .recipientName(context.recipientName())
            .recipientSchoolName(context.recipientSchoolName())
            .gisuId(context.gisuId())
            .gisuGeneration(context.gisuGeneration())
            .projectId(context.projectId())
            .projectName(context.projectName())
            .meritTitle(context.meritTitle())
            .meritDescription(context.meritDescription())
            .issuedByMemberId(context.issuedByMemberId())
            .issuedAt(issuedAt)
            .fileId(fileInfo.fileId())
            .fileSha256(fileSha256)
            .build());

        return CertificateIssueInfo.from(saveCertificatePort.save(certificate));
    }

    private byte[] renderPdf(CertificateIssueContext context, String serialNumber, Instant issuedAt, Instant expiresAt) {
        return renderCertificatePdfPort.render(CertificatePdfRenderCommand.builder()
            .issuanceNumber(serialNumber)
            .type(context.type())
            .template(context.template())
            .issuer(context.issuer())
            .recipientName(context.recipientName())
            .recipientSchoolName(context.recipientSchoolName())
            .gisuGeneration(context.gisuGeneration())
            .projectName(context.projectName())
            .meritTitle(context.meritTitle())
            .meritDescription(context.meritDescription())
            .issuedAt(issuedAt)
            .expiresAt(expiresAt)
            .verificationUrl(certificateProperties.verificationUrl(serialNumber))
            .build());
    }

    private <T> T executeInTransaction(Supplier<T> supplier) {
        return Objects.requireNonNull(transactionOperations.execute(status -> supplier.get()));
    }

    private String generateUniqueSerialNumber(CertificateIssueContext context, Instant issuedAt) {
        for (int attempt = 0; attempt < SERIAL_GENERATION_RETRY_COUNT; attempt++) {
            String serialNumber = serialNumberGenerator.generate(context.type(), issuedAt);
            if (!loadCertificatePort.existsBySerialNumber(serialNumber)) {
                return serialNumber;
            }
        }
        throw new CertificateException(CertificateErrorCode.CERTIFICATE_SERIAL_GENERATION_FAILED);
    }

    private void validateAdmin(Long memberId, Long gisuId) {
        if (getChallengerRoleUseCase.isSuperAdmin(memberId)
            || getChallengerRoleUseCase.isCentralCoreInGisu(memberId, gisuId)) {
            return;
        }
        throw new CertificateException(CertificateErrorCode.CERTIFICATE_ISSUE_FORBIDDEN);
    }

    private String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new CertificateException(CertificateErrorCode.CERTIFICATE_RENDER_FAILED, e);
        }
    }

    private record CertificateIssuePreparation(
        CertificateIssueInfo existingInfo,
        String serialNumber
    ) {

        private static CertificateIssuePreparation existing(CertificateIssueInfo existingInfo) {
            return new CertificateIssuePreparation(existingInfo, null);
        }

        private static CertificateIssuePreparation newIssue(String serialNumber) {
            return new CertificateIssuePreparation(null, serialNumber);
        }
    }
}
