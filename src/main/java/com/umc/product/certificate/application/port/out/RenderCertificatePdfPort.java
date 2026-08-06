package com.umc.product.certificate.application.port.out;

import com.umc.product.certificate.application.port.out.dto.CertificatePdfRenderCommand;

public interface RenderCertificatePdfPort {

    byte[] render(CertificatePdfRenderCommand command);
}
