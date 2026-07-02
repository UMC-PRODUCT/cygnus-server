package com.umc.product.certificate.application.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.umc.product.certificate.application.port.in.query.GetCertificateUseCase;
import com.umc.product.certificate.application.port.in.query.dto.CertificateDownloadInfo;
import com.umc.product.certificate.application.port.in.query.dto.CertificateInfo;
import com.umc.product.certificate.application.port.in.query.dto.CertificateVerificationInfo;
import com.umc.product.certificate.application.port.out.LoadCertificatePort;
import com.umc.product.certificate.domain.Certificate;
import com.umc.product.certificate.domain.exception.CertificateErrorCode;
import com.umc.product.certificate.domain.exception.CertificateException;
import com.umc.product.storage.application.port.in.query.GetFileUseCase;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CertificateQueryService implements GetCertificateUseCase {

    private final LoadCertificatePort loadCertificatePort;
    private final GetFileUseCase getFileUseCase;
    private final Clock clock;

    @Override
    public List<CertificateInfo> listByMemberId(Long memberId) {
        Instant now = Instant.now(clock);
        return loadCertificatePort.listByRecipientMemberId(memberId).stream()
            .map(certificate -> CertificateInfo.from(certificate, now))
            .toList();
    }

    @Override
    public CertificateDownloadInfo getDownloadInfo(Long certificateId, Long requesterMemberId) {
        Certificate certificate = loadCertificatePort.getById(certificateId);
        if (!certificate.getRecipientMemberId().equals(requesterMemberId)) {
            throw new CertificateException(CertificateErrorCode.CERTIFICATE_ACCESS_FORBIDDEN);
        }
        if (!certificate.isValidAt(Instant.now(clock))) {
            throw new CertificateException(CertificateErrorCode.CERTIFICATE_EXPIRED_OR_REVOKED);
        }

        String downloadUrl = getFileUseCase.getById(certificate.getFileId()).fileLink();
        return CertificateDownloadInfo.of(
            certificate.getId(),
            certificate.getSerialNumber(),
            downloadUrl,
            certificate.getExpiresAt()
        );
    }

    @Override
    public CertificateVerificationInfo verifyBySerialNumber(String serialNumber) {
        return loadCertificatePort.findBySerialNumber(serialNumber)
            .map(certificate -> CertificateVerificationInfo.from(
                certificate,
                Instant.now(clock),
                maskName(certificate.getRecipientName())
            ))
            .orElseGet(CertificateVerificationInfo::notFound);
    }

    private String maskName(String name) {
        if (!StringUtils.hasText(name)) {
            return "";
        }
        name = name.trim();
        int[] codePoints = name.codePoints().toArray();
        if (codePoints.length == 1) {
            return "*";
        }
        if (codePoints.length == 2) {
            return new String(codePoints, 0, 1) + "*";
        }
        return new String(codePoints, 0, 1)
            + "*".repeat(codePoints.length - 2)
            + new String(codePoints, codePoints.length - 1, 1);
    }
}
