package com.umc.product.certificate.application.port.in.command;

import com.umc.product.certificate.application.port.in.command.dto.AdminIssueCertificateCommand;
import com.umc.product.certificate.application.port.in.command.dto.CertificateIssueInfo;

public interface AdminIssueCertificateUseCase {

    CertificateIssueInfo issueByAdmin(AdminIssueCertificateCommand command);
}
