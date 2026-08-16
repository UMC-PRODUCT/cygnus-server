package com.umc.product.certificate.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

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
            "UMC_DEMO_DAY_AWS_SPECIAL_PRIZE",
            "UMC_DEMO_DAY_BEST_CHALLENGER",
            "UMC_HACKATHON_CERTIFICATION_OF_COMPLETION",
            "UMC_HACKATHON_GRAND_PRIZE",
            "UMC_HACKATHON_FIRST_PRIZE",
            "UMC_HACKATHON_SECOND_PRIZE",
            "NEORDINARY_HACKATHON_GRAND_PRIZE",
            "NEORDINARY_HACKATHON_FIRST_PRIZE",
            "NEORDINARY_HACKATHON_SECOND_PRIZE",
            "NEORDINARY_HACKATHON_CERTIFICATION_OF_COMPLETION"
        );
    }

    @Test
    @DisplayName("템플릿은 일련번호 코드와 발급 정책과 기본 상명을 제공한다")
    void 템플릿은_일련번호_코드와_발급_정책과_기본_상명을_제공한다() {
        // when
        CertificateTemplate template = CertificateTemplate.UMC_DEMO_DAY_FIRST_PRIZE;

        // then
        assertThat(template.serialCode()).isEqualTo("MRT");
        assertThat(template.requiresGraduation()).isFalse();
        assertThat(template.supportsSelfIssue()).isFalse();
        assertThat(template.issuer()).isEqualTo(CertificateIssuer.UNIVERSITY_MAKEUS_CHALLENGE);
        assertThat(template.defaultMeritTitle()).isEqualTo("최우수상");
        assertThat(template.backgroundResourcePath()).endsWith(".pdf");
    }

    @Test
    @DisplayName("UMC 과정 수료증만 셀프 발급할 수 있다")
    void UMC_과정_수료증만_셀프_발급할_수_있다() {
        assertThat(CertificateTemplate.UMC_COURSE_COMPLETION.supportsSelfIssue()).isTrue();
        assertThat(Arrays.stream(CertificateTemplate.values())
            .filter(CertificateTemplate::supportsSelfIssue))
            .containsExactly(CertificateTemplate.UMC_COURSE_COMPLETION);
    }

    @ParameterizedTest
    @MethodSource("templateTextCases")
    @DisplayName("템플릿은 기획표 기준 인증서 문구를 생성한다")
    void 템플릿은_기획표_기준_인증서_문구를_생성한다(
        CertificateTemplate template,
        String englishSubtitle,
        String englishTitleLine2,
        String koreanTitle,
        String koreanSubtitle,
        String description
    ) {
        // given
        Long generation = 10L;

        // when & then
        assertThat(template.englishCertificateTitle()).isEqualTo(englishSubtitle);
        assertThat(template.englishTitleLine1(generation)).isEqualTo("10th " + template.brandName());
        assertThat(template.englishTitleLine2()).isEqualTo(englishTitleLine2);
        assertThat(template.koreanTitle(template.awardName())).isEqualTo(koreanTitle);
        assertThat(template.koreanSubtitle(generation, template.awardName())).isEqualTo(koreanSubtitle);
        assertThat(template.defaultDescription(generation, template.awardName())).isEqualTo(description);
    }

    private static Stream<Arguments> templateTextCases() {
        return Stream.of(
            Arguments.of(
                CertificateTemplate.UMC_COURSE_COMPLETION,
                "Certificate of Completion",
                "COMPLETION",
                "수료증",
                "10th UMC 수료증",
                "위 챌린저는 전국 대학생 IT 연합 동아리 University MakeUs Challenge 10기 과정을 성실히 수료하였기에 이 증서를 수여합니다."
            ),
            Arguments.of(
                CertificateTemplate.UMC_COURSE_MERIT,
                "Certificate of Completion",
                "APPRECIATION",
                "공로증",
                "10th UMC 공로증",
                "위 운영진은 전국 대학생 IT 연합 동아리 University MakeUs Challenge 10기 과정의 발전에 기여하였기에 이 증서를 수여합니다."
            ),
            Arguments.of(
                CertificateTemplate.UMC_DEMO_DAY_BEST_CHALLENGER,
                "Certificate of Award",
                "DEMO DAY",
                "베스트 챌린저",
                "10th UMC 베스트 챌린저",
                "위 챌린저는 전국 대학생 IT 연합 동아리 University MakeUs Challenge 10기 과정에서 최고의 역량과 성과를 보였기에 이 증서를 수여합니다."
            ),
            Arguments.of(
                CertificateTemplate.UMC_DEMO_DAY_AWS_SPECIAL_PRIZE,
                "Certificate of Award",
                "DEMO DAY",
                "AWS특별상",
                "10th UMC AWS특별상",
                "위 챌린저는 10th UMC DEMO DAY에서 AWS 기술 활용의 우수성을 인정받아 이 증서를 수여합니다."
            ),
            Arguments.of(
                CertificateTemplate.UMC_DEMO_DAY_GRAND_PRIZE,
                "Certificate of Award",
                "DEMO DAY",
                "데모데이 대상",
                "10th UMC 데모데이 대상",
                "위 챌린저는 10th UMC DEMO DAY에서 이와 같이 우수한 성적을 거두었기에 이 증서를 수여합니다."
            ),
            Arguments.of(
                CertificateTemplate.UMC_DEMO_DAY_FIRST_PRIZE,
                "Certificate of Award",
                "DEMO DAY",
                "데모데이 최우수상",
                "10th UMC 데모데이 최우수상",
                "위 챌린저는 10th UMC DEMO DAY에서 이와 같이 우수한 성적을 거두었기에 이 증서를 수여합니다."
            ),
            Arguments.of(
                CertificateTemplate.UMC_DEMO_DAY_SECOND_PRIZE,
                "Certificate of Award",
                "DEMO DAY",
                "데모데이 우수상",
                "10th UMC 데모데이 우수상",
                "위 챌린저는 10th UMC DEMO DAY에서 이와 같이 우수한 성적을 거두었기에 이 증서를 수여합니다."
            ),
            Arguments.of(
                CertificateTemplate.UMC_DEMO_DAY_PARTICIPATION_PRIZE,
                "Certificate of Award",
                "DEMO DAY",
                "데모데이 장려상",
                "10th UMC 데모데이 장려상",
                "위 챌린저는 10th UMC DEMO DAY에서 이와 같이 우수한 성적을 거두었기에 이 증서를 수여합니다."
            ),
            Arguments.of(
                CertificateTemplate.NEORDINARY_HACKATHON_CERTIFICATION_OF_COMPLETION,
                "Certificate of Completion",
                "HACKATHON",
                "해커톤 수료증",
                "10th Ne(O)rdinary 해커톤 수료증",
                "위 챌린저는 10th Ne(O)rdinary HACKATHON을 성실히 수료하였기에 이 증서를 수여합니다."
            ),
            Arguments.of(
                CertificateTemplate.NEORDINARY_HACKATHON_GRAND_PRIZE,
                "Certificate of Award",
                "HACKATHON",
                "해커톤 대상",
                "10th Ne(O)rdinary 해커톤 대상",
                "위 챌린저는 10th Ne(O)rdinary HACKATHON에서 이와 같이 우수한 성적을 거두었기에 이 증서를 수여합니다."
            ),
            Arguments.of(
                CertificateTemplate.NEORDINARY_HACKATHON_FIRST_PRIZE,
                "Certificate of Award",
                "HACKATHON",
                "해커톤 최우수상",
                "10th Ne(O)rdinary 해커톤 최우수상",
                "위 챌린저는 10th Ne(O)rdinary HACKATHON에서 이와 같이 우수한 성적을 거두었기에 이 증서를 수여합니다."
            ),
            Arguments.of(
                CertificateTemplate.NEORDINARY_HACKATHON_SECOND_PRIZE,
                "Certificate of Award",
                "HACKATHON",
                "해커톤 우수상",
                "10th Ne(O)rdinary 해커톤 우수상",
                "위 챌린저는 10th Ne(O)rdinary HACKATHON에서 이와 같이 우수한 성적을 거두었기에 이 증서를 수여합니다."
            ),
            Arguments.of(
                CertificateTemplate.UMC_HACKATHON_CERTIFICATION_OF_COMPLETION,
                "Certificate of Completion",
                "HACKATHON",
                "해커톤 수료증",
                "10th UMC 해커톤 수료증",
                "위 챌린저는 10th UMC HACKATHON을 성실히 수료하였기에 이 증서를 수여합니다."
            ),
            Arguments.of(
                CertificateTemplate.UMC_HACKATHON_GRAND_PRIZE,
                "Certificate of Award",
                "HACKATHON",
                "해커톤 대상",
                "10th UMC 해커톤 대상",
                "위 챌린저는 10th UMC HACKATHON에서 이와 같이 우수한 성적을 거두었기에 이 증서를 수여합니다."
            ),
            Arguments.of(
                CertificateTemplate.UMC_HACKATHON_FIRST_PRIZE,
                "Certificate of Award",
                "HACKATHON",
                "해커톤 최우수상",
                "10th UMC 해커톤 최우수상",
                "위 챌린저는 10th UMC HACKATHON에서 이와 같이 우수한 성적을 거두었기에 이 증서를 수여합니다."
            ),
            Arguments.of(
                CertificateTemplate.UMC_HACKATHON_SECOND_PRIZE,
                "Certificate of Award",
                "HACKATHON",
                "해커톤 우수상",
                "10th UMC 해커톤 우수상",
                "위 챌린저는 10th UMC HACKATHON에서 이와 같이 우수한 성적을 거두었기에 이 증서를 수여합니다."
            )
        );
    }
}
