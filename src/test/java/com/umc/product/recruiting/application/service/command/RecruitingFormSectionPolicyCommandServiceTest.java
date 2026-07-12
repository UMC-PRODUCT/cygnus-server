package com.umc.product.recruiting.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;
import java.util.Optional;

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
import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo.SectionWithQuestions;
import com.umc.product.recruiting.application.port.in.command.dto.AddRecruitingFormSectionPolicyCommand;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationFormPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingFormSectionPolicyPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingFormSectionPolicyPort;
import com.umc.product.recruiting.domain.RecruitingApplicationForm;
import com.umc.product.recruiting.domain.RecruitingFormSectionPolicy;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.RecruitingSeason;
import com.umc.product.recruiting.domain.enums.RecruitingFormSectionType;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

@ExtendWith(MockitoExtension.class)
class RecruitingFormSectionPolicyCommandServiceTest {

    @Mock
    LoadRecruitingApplicationFormPort loadApplicationFormPort;

    @Mock
    LoadRecruitingFormSectionPolicyPort loadPolicyPort;

    @Mock
    SaveRecruitingFormSectionPolicyPort savePolicyPort;

    @Mock
    GetFormUseCase getFormUseCase;

    @InjectMocks
    RecruitingFormSectionPolicyCommandService sut;

    @Test
    @DisplayName("Form 공개 조회로 section 소속을 확인하고 TRACK 정책을 저장한다")
    void addTrackPolicyAfterCheckingFormSection() {
        RecruitingApplicationForm form = applicationForm();
        given(loadPolicyPort.findByFormSectionId(10L)).willReturn(Optional.empty());
        given(loadApplicationFormPort.getById(100L)).willReturn(form);
        given(getFormUseCase.getFormWithStructure(500L)).willReturn(FormWithStructureInfo.builder()
            .formId(500L)
            .sections(List.of(SectionWithQuestions.builder().sectionId(10L).questions(List.of()).build()))
            .build());
        given(savePolicyPort.save(org.mockito.ArgumentMatchers.any())).willAnswer(invocation -> {
            RecruitingFormSectionPolicy policy = invocation.getArgument(0);
            ReflectionTestUtils.setField(policy, "id", 200L);
            return policy;
        });

        Long result = sut.addPolicy(AddRecruitingFormSectionPolicyCommand.builder()
            .applicationFormId(100L)
            .formSectionId(10L)
            .type(RecruitingFormSectionType.TRACK)
            .track(ChallengerTrack.PLAN)
            .build());

        assertThat(result).isEqualTo(200L);
    }

    @Test
    @DisplayName("section policy type이 없으면 TRACK으로 간주하지 않고 거부한다")
    void rejectMissingPolicyType() {
        RecruitingApplicationForm form = applicationForm();
        given(loadPolicyPort.findByFormSectionId(10L)).willReturn(Optional.empty());
        given(loadApplicationFormPort.getById(100L)).willReturn(form);
        given(getFormUseCase.getFormWithStructure(500L)).willReturn(FormWithStructureInfo.builder()
            .formId(500L)
            .sections(List.of(SectionWithQuestions.builder().sectionId(10L).questions(List.of()).build()))
            .build());

        assertThatThrownBy(() -> sut.addPolicy(AddRecruitingFormSectionPolicyCommand.builder()
            .applicationFormId(100L)
            .formSectionId(10L)
            .track(ChallengerTrack.PLAN)
            .build()))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_FORM_SECTION_POLICY_INVALID);
        then(savePolicyPort).shouldHaveNoInteractions();
    }

    private RecruitingApplicationForm applicationForm() {
        RecruitingRound round = RecruitingRound.createRegular(RecruitingSeason.create(1L, 10L));
        ReflectionTestUtils.setField(round, "recruitableTracks", List.of(ChallengerTrack.PLAN));
        return RecruitingApplicationForm.create(round, 500L);
    }
}
