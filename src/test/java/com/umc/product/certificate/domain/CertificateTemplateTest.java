package com.umc.product.certificate.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CertificateTemplateTest {

    @Test
    @DisplayName("인증서 템플릿 enum은 렌더 가능한 배경 하나를 의미한다")
    void 인증서_템플릿_enum은_렌더_가능한_배경_하나를_의미한다() {
        // when
        var templateNames = Arrays.stream(CertificateTemplate.values())
            .map(CertificateTemplate::name)
            .toList();

        // then
        assertThat(templateNames).containsExactlyInAnyOrder(
            "UMC_COURSE_COMPLETION",
            "UMC_COURSE_MERIT",
            "UMC_DEMO_DAY_GRAND_PRIZE",
            "UMC_DEMO_DAY_FIRST_PRIZE",
            "UMC_DEMO_DAY_SECOND_PRIZE",
            "UMC_DEMO_DAY_PARTICIPATION_PRIZE",
            "UMC_DEMO_DAY_BEST_PART_CHALLENGER",
            "UMC_DEMO_DAY_AWS_SPECIAL_PRIZE",
            "NEORDINARY_HACKATHON_GRAND_PRIZE",
            "NEORDINARY_HACKATHON_FIRST_PRIZE",
            "NEORDINARY_HACKATHON_SECOND_PRIZE",
            "NEORDINARY_HACKATHON_CERTIFICATION_OF_COMPLETION"
        );
    }

    @Test
    @DisplayName("템플릿은 인증서 종류와 발급 주체와 기본 상명을 제공한다")
    void 템플릿은_인증서_종류와_발급_주체와_기본_상명을_제공한다() {
        // when
        CertificateTemplate template = CertificateTemplate.UMC_DEMO_DAY_FIRST_PRIZE;

        // then
        assertThat(template.type()).isEqualTo(CertificateType.MERIT);
        assertThat(template.issuer()).isEqualTo(CertificateIssuer.UNIVERSITY_MAKEUS_CHALLENGE);
        assertThat(template.defaultMeritTitle()).isEqualTo("최우수상");
        assertThat(template.backgroundResourcePath()).endsWith(".pdf");
    }
}
