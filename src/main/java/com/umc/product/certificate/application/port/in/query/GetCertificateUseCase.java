package com.umc.product.certificate.application.port.in.query;

import java.util.List;

import com.umc.product.certificate.application.port.in.query.dto.CertificateDownloadInfo;
import com.umc.product.certificate.application.port.in.query.dto.CertificateInfo;
import com.umc.product.certificate.application.port.in.query.dto.CertificateVerificationInfo;

public interface GetCertificateUseCase {

    List<CertificateInfo> listByMemberId(Long memberId);

    CertificateDownloadInfo getDownloadInfo(Long certificateId, Long requesterMemberId);

    CertificateVerificationInfo verifyBySerialNumber(String serialNumber);
}
