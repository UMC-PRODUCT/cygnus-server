package com.umc.product.certificate.adapter.in.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.umc.product.certificate.adapter.in.web.dto.request.IssueCertificateRequest;
import com.umc.product.certificate.adapter.in.web.dto.response.CertificateDownloadResponse;
import com.umc.product.certificate.adapter.in.web.dto.response.CertificateIssueResponse;
import com.umc.product.certificate.adapter.in.web.dto.response.CertificateResponse;
import com.umc.product.certificate.adapter.in.web.dto.response.CertificateVerificationResponse;
import com.umc.product.certificate.application.port.in.command.IssueCertificateUseCase;
import com.umc.product.certificate.application.port.in.query.GetCertificateUseCase;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.global.security.annotation.Public;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/certificates")
@RequiredArgsConstructor
@Tag(name = "Certificate | 인증서", description = "인증서 발급, 다운로드, 진위 확인을 제공합니다.")
public class CertificateController {

    private final IssueCertificateUseCase issueCertificateUseCase;
    private final GetCertificateUseCase getCertificateUseCase;

    @PostMapping
    @Operation(operationId = "CERTIFICATE-001", summary = "본인 인증서 셀프 발급")
    public CertificateIssueResponse issue(
        @CurrentMember MemberPrincipal principal,
        @Valid @RequestBody IssueCertificateRequest request
    ) {
        return CertificateIssueResponse.from(issueCertificateUseCase.issue(
            request.toCommand(principal.getMemberId())
        ));
    }

    @GetMapping
    @Operation(operationId = "CERTIFICATE-002", summary = "본인 인증서 목록 조회")
    public List<CertificateResponse> list(@CurrentMember MemberPrincipal principal) {
        return getCertificateUseCase.listByMemberId(principal.getMemberId()).stream()
            .map(CertificateResponse::from)
            .toList();
    }

    @GetMapping("/{certificateId}/download")
    @Operation(operationId = "CERTIFICATE-003", summary = "본인 인증서 다운로드 URL 발급")
    public CertificateDownloadResponse download(
        @CurrentMember MemberPrincipal principal,
        @PathVariable Long certificateId
    ) {
        return CertificateDownloadResponse.from(
            getCertificateUseCase.getDownloadInfo(certificateId, principal.getMemberId())
        );
    }

    @Public
    @GetMapping("/verify/{serialNumber}")
    @Operation(operationId = "CERTIFICATE-004", summary = "인증서 진위 확인")
    public CertificateVerificationResponse verify(@PathVariable String serialNumber) {
        return CertificateVerificationResponse.from(getCertificateUseCase.verifyBySerialNumber(serialNumber));
    }
}
