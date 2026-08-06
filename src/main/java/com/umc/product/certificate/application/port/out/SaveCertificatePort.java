package com.umc.product.certificate.application.port.out;

import com.umc.product.certificate.domain.Certificate;

public interface SaveCertificatePort {

    Certificate save(Certificate certificate);
}
