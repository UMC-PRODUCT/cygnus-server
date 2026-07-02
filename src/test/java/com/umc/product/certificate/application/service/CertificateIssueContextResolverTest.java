package com.umc.product.certificate.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.certificate.application.port.in.command.dto.AdminIssueCertificateCommand;
import com.umc.product.certificate.application.port.in.command.dto.IssueCertificateCommand;
import com.umc.product.certificate.domain.CertificateIssuer;
import com.umc.product.certificate.domain.CertificateTemplate;
import com.umc.product.certificate.domain.CertificateType;
import com.umc.product.certificate.domain.exception.CertificateErrorCode;
import com.umc.product.certificate.domain.exception.CertificateException;
import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.common.domain.enums.ChallengerStatus;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;
import com.umc.product.project.application.port.in.query.GetProjectMemberUseCase;

@ExtendWith(MockitoExtension.class)
class CertificateIssueContextResolverTest {

    @Mock
    GetMemberUseCase getMemberUseCase;

    @Mock
    GetChallengerUseCase getChallengerUseCase;

    @Mock
    GetGisuUseCase getGisuUseCase;

    @Mock
    GetProjectMemberUseCase getProjectMemberUseCase;

    @Test
    @DisplayName("수료증 외 인증서는 셀프 발급을 허용하지 않는다")
    void 수료증_외_인증서는_셀프_발급을_허용하지_않는다() {
        // given
        CertificateIssueContextResolver sut = sut();

        // when & then
        assertThatThrownBy(() -> sut.resolveSelf(IssueCertificateCommand.builder()
            .type(CertificateType.MERIT)
            .requesterMemberId(1L)
            .gisuId(7L)
            .build()))
            .isInstanceOf(CertificateException.class)
            .extracting("baseCode")
            .isEqualTo(CertificateErrorCode.CERTIFICATE_SELF_ISSUE_FORBIDDEN);
    }

    @Test
    @DisplayName("셀프 수료증은 UMC 과정 수료증 템플릿을 사용한다")
    void 셀프_수료증은_UMC_과정_수료증_템플릿을_사용한다() {
        // given
        given(getMemberUseCase.getById(1L)).willReturn(member());
        given(getGisuUseCase.getById(7L)).willReturn(gisu());
        given(getChallengerUseCase.findByMemberIdAndGisuId(1L, 7L)).willReturn(Optional.of(
            ChallengerInfo.builder()
                .challengerId(10L)
                .memberId(1L)
                .gisuId(7L)
                .challengerStatus(ChallengerStatus.GRADUATED)
                .build()
        ));
        CertificateIssueContextResolver sut = sut();

        // when
        CertificateIssueContext result = sut.resolveSelf(IssueCertificateCommand.builder()
            .type(CertificateType.COMPLETION)
            .requesterMemberId(1L)
            .gisuId(7L)
            .build());

        // then
        assertThat(result.template()).isEqualTo(CertificateTemplate.UMC_COURSE_COMPLETION);
    }

    @Test
    @DisplayName("운영진 공로증 발급 시 템플릿으로 발급 주체를 설정한다")
    void 운영진_공로증_발급_시_템플릿으로_발급_주체를_설정한다() {
        // given
        given(getMemberUseCase.getById(1L)).willReturn(member());
        given(getGisuUseCase.getById(7L)).willReturn(gisu());
        CertificateIssueContextResolver sut = sut();

        // when
        CertificateIssueContext result = sut.resolveAdmin(AdminIssueCertificateCommand.builder()
            .template(CertificateTemplate.NEORDINARY_HACKATHON_GRAND_PRIZE)
            .requesterMemberId(99L)
            .recipientMemberId(1L)
            .gisuId(7L)
            .build());

        // then
        assertThat(result.type()).isEqualTo(CertificateType.MERIT);
        assertThat(result.issuer()).isEqualTo(CertificateIssuer.NEORDINARY);
        assertThat(result.gisuGeneration()).isEqualTo(7L);
        assertThat(result.meritTitle()).isEqualTo("대상");
    }

    @Test
    @DisplayName("운영진 템플릿 발급은 템플릿으로 종류와 발급 주체와 기본 상명을 결정한다")
    void 운영진_템플릿_발급은_템플릿으로_종류와_발급_주체와_기본_상명을_결정한다() {
        // given
        given(getMemberUseCase.getById(1L)).willReturn(member());
        given(getGisuUseCase.getById(7L)).willReturn(gisu());
        CertificateIssueContextResolver sut = sut();

        // when
        CertificateIssueContext result = sut.resolveAdmin(AdminIssueCertificateCommand.builder()
            .template(CertificateTemplate.UMC_DEMO_DAY_FIRST_PRIZE)
            .requesterMemberId(99L)
            .recipientMemberId(1L)
            .gisuId(7L)
            .build());

        // then
        assertThat(result.template()).isEqualTo(CertificateTemplate.UMC_DEMO_DAY_FIRST_PRIZE);
        assertThat(result.type()).isEqualTo(CertificateType.MERIT);
        assertThat(result.issuer()).isEqualTo(CertificateIssuer.UNIVERSITY_MAKEUS_CHALLENGE);
        assertThat(result.meritTitle()).isEqualTo("최우수상");
    }

    @Test
    @DisplayName("커스텀 상장은 기존 템플릿 배경과 입력 문구를 사용한다")
    void 커스텀_상장은_기존_템플릿_배경과_입력_문구를_사용한다() {
        // given
        given(getMemberUseCase.getById(1L)).willReturn(member());
        given(getGisuUseCase.getById(7L)).willReturn(gisu());
        CertificateIssueContextResolver sut = sut();

        // when
        CertificateIssueContext result = sut.resolveAdmin(AdminIssueCertificateCommand.builder()
            .template(CertificateTemplate.UMC_DEMO_DAY_SECOND_PRIZE)
            .requesterMemberId(99L)
            .recipientMemberId(1L)
            .gisuId(7L)
            .meritTitle("커스텀 공로상")
            .meritDescription("커스텀 설명입니다.")
            .build());

        // then
        assertThat(result.template()).isEqualTo(CertificateTemplate.UMC_DEMO_DAY_SECOND_PRIZE);
        assertThat(result.meritTitle()).isEqualTo("커스텀 공로상");
        assertThat(result.meritDescription()).isEqualTo("커스텀 설명입니다.");
    }

    @Test
    @DisplayName("수료 상태가 아니면 수료증 발급 조건을 만족하지 않는다")
    void 수료_상태가_아니면_수료증_발급_조건을_만족하지_않는다() {
        // given
        given(getMemberUseCase.getById(1L)).willReturn(member());
        given(getGisuUseCase.getById(7L)).willReturn(gisu());
        given(getChallengerUseCase.findByMemberIdAndGisuId(1L, 7L)).willReturn(Optional.of(
            ChallengerInfo.builder()
                .challengerId(10L)
                .memberId(1L)
                .gisuId(7L)
                .challengerStatus(ChallengerStatus.ACTIVE)
                .build()
        ));
        CertificateIssueContextResolver sut = sut();

        // when & then
        assertThatThrownBy(() -> sut.resolveSelf(IssueCertificateCommand.builder()
            .type(CertificateType.COMPLETION)
            .requesterMemberId(1L)
            .gisuId(7L)
            .build()))
            .isInstanceOf(CertificateException.class)
            .extracting("baseCode")
            .isEqualTo(CertificateErrorCode.CERTIFICATE_ELIGIBILITY_NOT_MET);
    }

    @Test
    @DisplayName("프로젝트 참가 확인서는 전용 PDF 템플릿이 없어 셀프 발급을 허용하지 않는다")
    void 프로젝트_참가_확인서는_전용_PDF_템플릿이_없어_셀프_발급을_허용하지_않는다() {
        // given
        CertificateIssueContextResolver sut = sut();

        // when & then
        assertThatThrownBy(() -> sut.resolveSelf(IssueCertificateCommand.builder()
            .type(CertificateType.PROJECT_PARTICIPATION)
            .requesterMemberId(1L)
            .gisuId(7L)
            .projectId(100L)
            .build()))
            .isInstanceOf(CertificateException.class)
            .extracting("baseCode")
            .isEqualTo(CertificateErrorCode.CERTIFICATE_SELF_ISSUE_FORBIDDEN);
    }

    @Test
    @DisplayName("템플릿 공로증 제목이 없으면 기본 상명을 사용한다")
    void 템플릿_공로증_제목이_없으면_기본_상명을_사용한다() {
        // given
        given(getMemberUseCase.getById(1L)).willReturn(member());
        given(getGisuUseCase.getById(7L)).willReturn(gisu());
        CertificateIssueContextResolver sut = sut();

        // when
        CertificateIssueContext result = sut.resolveAdmin(AdminIssueCertificateCommand.builder()
            .template(CertificateTemplate.UMC_COURSE_MERIT)
            .requesterMemberId(99L)
            .recipientMemberId(1L)
            .gisuId(7L)
            .meritTitle(" ")
            .build());

        // then
        assertThat(result.meritTitle()).isEqualTo("공로증");
    }

    private CertificateIssueContextResolver sut() {
        return new CertificateIssueContextResolver(
            getMemberUseCase,
            getChallengerUseCase,
            getGisuUseCase,
            getProjectMemberUseCase
        );
    }

    private MemberInfo member() {
        return MemberInfo.builder()
            .id(1L)
            .name("김유엠")
            .schoolName("유엠씨대학교")
            .build();
    }

    private GisuInfo gisu() {
        return new GisuInfo(7L, 7L, Instant.parse("2026-03-01T00:00:00Z"), Instant.parse("2026-08-31T00:00:00Z"), true);
    }

}
