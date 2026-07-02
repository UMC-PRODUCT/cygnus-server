package com.umc.product.certificate.application.port.in.query;

import com.umc.product.certificate.application.port.in.query.dto.CertificatePdfPreviewInfo;
import com.umc.product.certificate.application.port.in.query.dto.CertificatePdfPreviewQuery;

public interface PreviewCertificatePdfUseCase {

    CertificatePdfPreviewInfo preview(CertificatePdfPreviewQuery query);
}
