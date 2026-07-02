package com.umc.product.certificate.application.port.in.command;

import com.umc.product.certificate.application.port.in.command.dto.CertificateIssueInfo;
import com.umc.product.certificate.application.port.in.command.dto.IssueCertificateCommand;

public interface IssueCertificateUseCase {

    CertificateIssueInfo issue(IssueCertificateCommand command);
}
