package com.umc.product.certificate.adapter.in.graphql.dto;

import com.umc.product.certificate.application.port.in.command.dto.IssueCertificateCommand;
import com.umc.product.certificate.domain.CertificateTemplate;

public record IssueCertificateGraphQlRequest(
    CertificateTemplate template,
    Long gisuId
) {

    public IssueCertificateCommand toCommand(Long requesterMemberId) {
        return IssueCertificateCommand.builder()
            .template(template)
            .gisuId(gisuId)
            .requesterMemberId(requesterMemberId)
            .build();
    }
}
