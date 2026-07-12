package com.umc.product.recruiting.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo.QuestionWithOptions;
import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo.SectionWithQuestions;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationQuestionScopeInfo;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationFormPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingFormSectionPolicyPort;
import com.umc.product.recruiting.domain.RecruitingApplicationForm;
import com.umc.product.recruiting.domain.RecruitingFormSectionPolicy;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.RecruitingRoundConfiguration;
import com.umc.product.recruiting.domain.RecruitingSeason;

@ExtendWith(MockitoExtension.class)
class RecruitingApplicationQuestionScopeQueryServiceTest {

    @Mock
    LoadRecruitingApplicationFormPort loadApplicationFormPort;

    @Mock
    LoadRecruitingFormSectionPolicyPort loadPolicyPort;

    @Mock
    GetFormUseCase getFormUseCase;

    @InjectMocks
    RecruitingApplicationQuestionScopeQueryService sut;

    @Test
    @DisplayName("COMMON과 1지망 및 2지망 섹션 문항만 허용하고 required 문항만 필수로 반환한다")
    void resolveSelectedQuestionScope() {
        RecruitingApplicationForm form = applicationForm();
        given(loadApplicationFormPort.getById(100L)).willReturn(form);
        given(loadPolicyPort.listByApplicationFormId(100L)).willReturn(List.of(
            RecruitingFormSectionPolicy.createCommon(form, 10L),
            RecruitingFormSectionPolicy.createTrack(form, 11L, ChallengerTrack.PLAN),
            RecruitingFormSectionPolicy.createTrack(form, 12L, ChallengerTrack.DESIGN),
            RecruitingFormSectionPolicy.createTrack(form, 13L, ChallengerTrack.MOBILE_PRODUCT_ENGINEER)
        ));
        given(getFormUseCase.getFormWithStructure(500L)).willReturn(formStructure());

        RecruitingApplicationQuestionScopeInfo result = sut.getQuestionScope(
            100L,
            ChallengerTrack.PLAN,
            ChallengerTrack.DESIGN
        );

        assertThat(result.allowedQuestionIds()).containsExactlyInAnyOrder(1L, 2L, 3L, 4L);
        assertThat(result.requiredQuestionIds()).containsExactlyInAnyOrder(1L, 3L);
    }

    private RecruitingApplicationForm applicationForm() {
        RecruitingRound round = RecruitingRound.createRegular(
            RecruitingSeason.create(1L, 10L),
            RecruitingRoundConfiguration.of(
                List.of(ChallengerTrack.PLAN, ChallengerTrack.DESIGN, ChallengerTrack.MOBILE_PRODUCT_ENGINEER),
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
            )
        );
        RecruitingApplicationForm form = RecruitingApplicationForm.create(round, 500L);
        ReflectionTestUtils.setField(form, "id", 100L);
        return form;
    }

    private FormWithStructureInfo formStructure() {
        return FormWithStructureInfo.builder()
            .formId(500L)
            .sections(List.of(
                section(10L, question(1L, true), question(2L, false)),
                section(11L, question(3L, true)),
                section(12L, question(4L, false)),
                section(13L, question(5L, true))
            ))
            .build();
    }

    private SectionWithQuestions section(Long sectionId, QuestionWithOptions... questions) {
        return SectionWithQuestions.builder()
            .sectionId(sectionId)
            .questions(List.of(questions))
            .build();
    }

    private QuestionWithOptions question(Long questionId, boolean required) {
        return QuestionWithOptions.builder()
            .questionId(questionId)
            .isRequired(required)
            .options(List.of())
            .build();
    }
}
