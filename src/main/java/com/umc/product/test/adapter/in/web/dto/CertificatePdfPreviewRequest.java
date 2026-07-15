package com.umc.product.test.adapter.in.web.dto;

import org.springframework.util.StringUtils;

import com.umc.product.certificate.application.port.in.query.dto.CertificatePdfPreviewQuery;
import com.umc.product.certificate.domain.CertificateTemplate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CertificatePdfPreviewRequest(
    @NotNull CertificateTemplate template,
    String issuanceNumber,
    String recipientName,
    String recipientSchoolName,
    @Positive Long gisuGeneration,
    String meritTitle,
    String meritDescription,
    String verificationUrl
) {

    private static final Long DEFAULT_GISU_GENERATION = 7L;
    private static final String DEFAULT_RECIPIENT_NAME = "김유엠";
    private static final String DEFAULT_RECIPIENT_SCHOOL_NAME = "유엠씨대학교";
    public CertificatePdfPreviewRequest {
        issuanceNumber = trimToNull(issuanceNumber);
        recipientName = defaultText(recipientName, DEFAULT_RECIPIENT_NAME);
        recipientSchoolName = defaultText(recipientSchoolName, DEFAULT_RECIPIENT_SCHOOL_NAME);
        gisuGeneration = gisuGeneration == null ? DEFAULT_GISU_GENERATION : gisuGeneration;
        meritTitle = trimToNull(meritTitle);
        meritDescription = trimToNull(meritDescription);
        verificationUrl = trimToNull(verificationUrl);
    }

    public CertificatePdfPreviewQuery toQuery(String requestOrigin) {
        return CertificatePdfPreviewQuery.builder()
            .template(template)
            .issuanceNumber(resolveIssuanceNumber())
            .recipientName(recipientName)
            .recipientSchoolName(recipientSchoolName)
            .gisuGeneration(gisuGeneration)
            .meritTitle(meritTitle)
            .meritDescription(meritDescription)
            .verificationUrl(verificationUrl)
            .requestOrigin(requestOrigin)
            .build();
    }

    private String resolveIssuanceNumber() {
        if (StringUtils.hasText(issuanceNumber)) {
            return issuanceNumber;
        }
        return "UMC-" + template.serialCode() + "-20260703-SAMPLE01";
    }

    private static String defaultText(String value, String defaultValue) {
        String trimmedValue = trimToNull(value);
        return trimmedValue == null ? defaultValue : trimmedValue;
    }

    private static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
