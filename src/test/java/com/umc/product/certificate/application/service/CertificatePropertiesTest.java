package com.umc.product.certificate.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CertificatePropertiesTest {

    @Test
    @DisplayName("검증 URL 설정이 비어 있으면 기본 경로를 사용한다")
    void 검증_URL_설정이_비어_있으면_기본_경로를_사용한다() {
        CertificateProperties properties = new CertificateProperties(" ");

        assertThat(properties.verificationUrl("SERIAL/1"))
            .isEqualTo("/api/v1/certificates/verify/SERIAL%2F1");
    }
}
