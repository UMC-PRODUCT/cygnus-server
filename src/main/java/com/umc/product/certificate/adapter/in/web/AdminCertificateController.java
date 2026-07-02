package com.umc.product.certificate.adapter.in.web;

import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.umc.product.certificate.adapter.in.web.dto.request.AdminIssueCertificateRequest;
import com.umc.product.certificate.adapter.in.web.dto.request.RevokeCertificateRequest;
import com.umc.product.certificate.adapter.in.web.dto.response.CertificateIssueResponse;
import com.umc.product.certificate.application.port.in.command.AdminIssueCertificateUseCase;
import com.umc.product.certificate.application.port.in.command.RevokeCertificateUseCase;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/certificates/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Certificate | 인증서 운영", description = "운영진 인증서 발급과 폐기를 제공합니다.")
public class AdminCertificateController {

    private final AdminIssueCertificateUseCase adminIssueCertificateUseCase;
    private final RevokeCertificateUseCase revokeCertificateUseCase;

    @PostMapping
    @Operation(operationId = "ADMIN-CERTIFICATE-001", summary = "운영진 인증서 발급")
    public CertificateIssueResponse issue(
        @CurrentMember MemberPrincipal principal,
        @Valid @RequestBody AdminIssueCertificateRequest request
    ) {
        return CertificateIssueResponse.from(adminIssueCertificateUseCase.issueByAdmin(
            request.toCommand(principal.getMemberId())
        ));
    }

    @PatchMapping("/{certificateId}/revoke")
    @Operation(operationId = "ADMIN-CERTIFICATE-002", summary = "운영진 인증서 폐기")
    public void revoke(
        @CurrentMember MemberPrincipal principal,
        @PathVariable Long certificateId,
        @Valid @RequestBody RevokeCertificateRequest request
    ) {
        revokeCertificateUseCase.revoke(request.toCommand(certificateId, principal.getMemberId()));
    }
}
