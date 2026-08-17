package com.umc.product.certificate.application.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.umc.product.certificate.application.port.in.query.PreviewCertificatePdfUseCase;
import com.umc.product.certificate.application.port.in.query.dto.CertificatePdfPreviewInfo;
import com.umc.product.certificate.application.port.in.query.dto.CertificatePdfPreviewQuery;
import com.umc.product.certificate.application.port.out.RenderCertificatePdfPort;
import com.umc.product.certificate.application.port.out.dto.CertificatePdfRenderCommand;

import lombok.RequiredArgsConstructor;

@Service
@Profile("local | alpha")
@RequiredArgsConstructor
public class CertificatePdfPreviewService implements PreviewCertificatePdfUseCase {

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    private final RenderCertificatePdfPort renderCertificatePdfPort;
    private final CertificateProperties certificateProperties;

    @Override
    public CertificatePdfPreviewInfo preview(CertificatePdfPreviewQuery query) {
        Instant issuedAt = LocalDate.now(KOREA_ZONE).atStartOfDay(KOREA_ZONE).toInstant();
        String verificationUrl = resolveVerificationUrl(query);
        byte[] content = renderCertificatePdfPort.render(CertificatePdfRenderCommand.builder()
            .issuanceNumber(query.issuanceNumber())
            .template(query.template())
            .recipientName(query.recipientName())
            .recipientSchoolName(query.recipientSchoolName())
            .gisuGeneration(query.gisuGeneration())
            .meritTitle(query.meritTitle())
            .meritDescription(query.meritDescription())
            .issuedAt(issuedAt)
            .expiresAt(issuedAt.plusSeconds(365L * 24L * 60L * 60L))
            .verificationUrl(verificationUrl)
            .build());

        return CertificatePdfPreviewInfo.of(fileName(query), content);
    }

    private String resolveVerificationUrl(CertificatePdfPreviewQuery query) {
        String verificationUrl = StringUtils.hasText(query.verificationUrl())
            ? query.verificationUrl().trim()
            : certificateProperties.verificationUrl(query.issuanceNumber());

        if (!verificationUrl.startsWith("/") || !StringUtils.hasText(query.requestOrigin())) {
            return verificationUrl;
        }
        return query.requestOrigin().replaceAll("/+$", "") + verificationUrl;
    }

    private String fileName(CertificatePdfPreviewQuery query) {
        return query.template().name().toLowerCase() + "-" + query.issuanceNumber() + ".pdf";
    }
}
