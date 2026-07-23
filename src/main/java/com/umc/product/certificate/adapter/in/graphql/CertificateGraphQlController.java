package com.umc.product.certificate.adapter.in.graphql;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import com.umc.product.certificate.adapter.in.graphql.dto.IssueCertificateGraphQlRequest;
import com.umc.product.certificate.application.port.in.command.IssueCertificateUseCase;
import com.umc.product.certificate.application.port.in.query.GetCertificateUseCase;
import com.umc.product.certificate.application.port.in.query.dto.CertificateDownloadInfo;
import com.umc.product.certificate.application.port.in.query.dto.CertificateInfo;
import com.umc.product.certificate.application.port.in.query.dto.CertificateVerificationInfo;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.organization.adapter.in.graphql.dto.GisuGraphQlResponse;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class CertificateGraphQlController {

    private final IssueCertificateUseCase issueCertificateUseCase;
    private final GetCertificateUseCase getCertificateUseCase;
    private final GetGisuUseCase getGisuUseCase;

    @QueryMapping
    public List<CertificateInfo> myCertificates(@CurrentMember MemberPrincipal principal) {
        return getCertificateUseCase.listByMemberId(principal.getMemberId());
    }

    @QueryMapping
    public CertificateVerificationInfo certificateVerification(@Argument String serialNumber) {
        return getCertificateUseCase.verifyBySerialNumber(serialNumber);
    }

    @MutationMapping
    public CertificateInfo issueCertificate(
        @CurrentMember MemberPrincipal principal,
        @Argument IssueCertificateGraphQlRequest input
    ) {
        Long certificateId = issueCertificateUseCase.issue(input.toCommand(principal.getMemberId())).certificateId();
        return getCertificateUseCase.listByMemberId(principal.getMemberId()).stream()
            .filter(certificate -> certificate.certificateId().equals(certificateId))
            .findFirst()
            .orElseThrow();
    }

    @SchemaMapping(typeName = "Certificate", field = "download")
    public CertificateDownloadInfo download(
        CertificateInfo certificate,
        @CurrentMember MemberPrincipal principal
    ) {
        return getCertificateUseCase.getDownloadInfo(certificate.certificateId(), principal.getMemberId());
    }

    @BatchMapping(typeName = "Certificate", field = "gisu")
    public Map<CertificateInfo, GisuGraphQlResponse> gisus(List<CertificateInfo> certificates) {
        Set<Long> gisuIds = certificates.stream()
            .map(CertificateInfo::gisuId)
            .collect(Collectors.toSet());
        Map<Long, GisuInfo> gisusById = getGisuUseCase.getByIds(gisuIds).stream()
            .collect(Collectors.toMap(GisuInfo::gisuId, Function.identity()));

        return certificates.stream().collect(Collectors.toMap(
            Function.identity(),
            certificate -> GisuGraphQlResponse.from(gisusById.get(certificate.gisuId()))
        ));
    }
}
