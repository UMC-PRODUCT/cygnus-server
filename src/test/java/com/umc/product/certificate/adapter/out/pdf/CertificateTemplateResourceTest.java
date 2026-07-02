package com.umc.product.certificate.adapter.out.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import com.umc.product.certificate.domain.CertificateTemplate;

class CertificateTemplateResourceTest {

    @Test
    @DisplayName("모든 인증서 템플릿 배경과 폰트 리소스를 classpath에서 읽을 수 있다")
    void 모든_인증서_템플릿_배경과_폰트_리소스를_classpath에서_읽을_수_있다() {
        // when & then
        for (CertificateTemplate template : CertificateTemplate.values()) {
            assertThat(new ClassPathResource(template.backgroundResourcePath()).exists())
                .as(template.name())
                .isTrue();
        }
        assertThat(new ClassPathResource("certificate/config/certificate_template.json").exists()).isTrue();
        assertThat(new ClassPathResource("certificate/fonts/Pretendard-Regular.ttf").exists()).isTrue();
        assertThat(new ClassPathResource("certificate/fonts/Pretendard-Medium.ttf").exists()).isTrue();
        assertThat(new ClassPathResource("certificate/fonts/Pretendard-SemiBold.ttf").exists()).isTrue();
    }
}
