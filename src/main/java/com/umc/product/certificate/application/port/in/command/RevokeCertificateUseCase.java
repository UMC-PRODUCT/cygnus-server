package com.umc.product.certificate.application.port.in.command;

import com.umc.product.certificate.application.port.in.command.dto.RevokeCertificateCommand;

public interface RevokeCertificateUseCase {

    void revoke(RevokeCertificateCommand command);
}
