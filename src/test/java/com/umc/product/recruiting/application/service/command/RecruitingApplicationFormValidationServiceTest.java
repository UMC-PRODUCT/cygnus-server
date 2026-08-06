package com.umc.product.recruiting.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.form.application.port.in.query.GetFormUseCase;
import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationFormPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingFormSectionPolicyPort;
import com.umc.product.recruiting.domain.RecruitingApplicationForm;
import com.umc.product.recruiting.domain.RecruitingFormSectionPolicy;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.RecruitingRoundConfiguration;
import com.umc.product.recruiting.domain.RecruitingSeason;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

@ExtendWith(MockitoExtension.class)
class RecruitingApplicationFormValidationServiceTest {

    @Mock
    LoadRecruitingApplicationFormPort loadApplicationFormPort;

    @Mock
    LoadRecruitingFormSectionPolicyPort loadPolicyPort;

    @Mock
    GetFormUseCase getFormUseCase;

    @InjectMocks
    RecruitingApplicationFormValidationService sut;

    @Test
    @DisplayName("게시할 Form에 정책 없는 section이 있으면 거부한다")
    void rejectSectionWithoutPolicy() {
        RecruitingApplicationForm form = applicationForm();
        List<RecruitingFormSectionPolicy> policies = List.of(
            RecruitingFormSectionPolicy.createTrack(form, 1L, ChallengerTrack.PLAN),
            RecruitingFormSectionPolicy.createTrack(form, 2L, ChallengerTrack.DESIGN)
        );
        given(loadApplicationFormPort.getById(100L)).willReturn(form);
        given(loadPolicyPort.listByApplicationFormId(100L)).willReturn(policies);
        given(getFormUseCase.getFormWithStructure(500L)).willReturn(structure(
            section(1L, null),
            section(2L, null),
            section(3L, null)
        ));

        assertThatThrownBy(() -> sut.validateForPublish(100L))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_FORM_SECTION_POLICY_INVALID);
    }

    @Test
    @DisplayName("TRACK section의 조건부 이동이 다른 TRACK section을 향하면 거부한다")
    void rejectConditionalTransitionToDifferentTrack() {
        RecruitingApplicationForm form = applicationForm();
        List<RecruitingFormSectionPolicy> policies = List.of(
            RecruitingFormSectionPolicy.createTrack(form, 1L, ChallengerTrack.PLAN),
            RecruitingFormSectionPolicy.createTrack(form, 2L, ChallengerTrack.DESIGN)
        );
        given(loadApplicationFormPort.getById(100L)).willReturn(form);
        given(loadPolicyPort.listByApplicationFormId(100L)).willReturn(policies);
        given(getFormUseCase.getFormWithStructure(500L)).willReturn(structure(
            section(1L, 2L),
            section(2L, null)
        ));

        assertThatThrownBy(() -> sut.validateForPublish(100L))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_FORM_SECTION_POLICY_INVALID);
    }

    @Test
    @DisplayName("게시 검증이 완료되면 Form의 TRACK section 집합을 반환한다")
    void returnValidatedTrackSections() {
        RecruitingApplicationForm form = applicationForm();
        List<RecruitingFormSectionPolicy> policies = List.of(
            RecruitingFormSectionPolicy.createCommon(form, 1L),
            RecruitingFormSectionPolicy.createTrack(form, 2L, ChallengerTrack.PLAN),
            RecruitingFormSectionPolicy.createTrack(form, 3L, ChallengerTrack.DESIGN)
        );
        given(loadApplicationFormPort.getById(100L)).willReturn(form);
        given(loadPolicyPort.listByApplicationFormId(100L)).willReturn(policies);
        given(getFormUseCase.getFormWithStructure(500L)).willReturn(structure(
            section(1L, null),
            section(2L, null),
            section(3L, null)
        ));

        assertThat(sut.validateForPublish(100L))
            .containsExactlyInAnyOrder(ChallengerTrack.PLAN, ChallengerTrack.DESIGN);
    }

    private RecruitingApplicationForm applicationForm() {
        RecruitingSeason season = RecruitingSeason.create(1L, 10L);
        RecruitingRound round = RecruitingRound.createRegular(season, RecruitingRoundConfiguration.of(
            List.of(ChallengerTrack.PLAN, ChallengerTrack.DESIGN),
            true,
            Instant.parse("2026-08-01T00:00:00Z"),
            Instant.parse("2026-08-08T00:00:00Z"),
            Instant.parse("2026-08-10T00:00:00Z"),
            false,
            null,
            null,
            Instant.parse("2026-08-16T00:00:00Z"),
            null,
            null,
            null
        ));
        RecruitingApplicationForm form = RecruitingApplicationForm.create(round, 500L);
        ReflectionTestUtils.setField(form, "id", 100L);
        return form;
    }

    private FormWithStructureInfo structure(FormWithStructureInfo.SectionWithQuestions... sections) {
        return FormWithStructureInfo.builder()
            .formId(500L)
            .sections(List.of(sections))
            .build();
    }

    private FormWithStructureInfo.SectionWithQuestions section(Long sectionId, Long nextSectionId) {
        List<FormWithStructureInfo.Option> options = nextSectionId == null
            ? List.of()
            : List.of(FormWithStructureInfo.Option.builder()
                .optionId(1000L + sectionId)
                .nextSectionId(nextSectionId)
                .build());
        return FormWithStructureInfo.SectionWithQuestions.builder()
            .sectionId(sectionId)
            .questions(List.of(FormWithStructureInfo.QuestionWithOptions.builder()
                .questionId(100L + sectionId)
                .options(options)
                .build()))
            .build();
    }
}
