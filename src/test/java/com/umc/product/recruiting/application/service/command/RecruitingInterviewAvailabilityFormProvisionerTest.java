package com.umc.product.recruiting.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.form.application.port.in.command.ManageFormSectionUseCase;
import com.umc.product.form.application.port.in.command.ManageFormUseCase;
import com.umc.product.form.application.port.in.command.ManageQuestionUseCase;
import com.umc.product.form.application.port.in.command.dto.CreateDraftFormCommand;
import com.umc.product.form.application.port.in.command.dto.CreateFormSectionCommand;
import com.umc.product.form.application.port.in.command.dto.CreateQuestionCommand;
import com.umc.product.form.application.port.in.command.dto.PublishFormCommand;
import com.umc.product.form.domain.enums.QuestionType;
import com.umc.product.recruiting.application.port.in.command.dto.RecruitingRoundConfigurationCommand;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.RecruitingSeason;

@ExtendWith(MockitoExtension.class)
class RecruitingInterviewAvailabilityFormProvisionerTest {

    @Mock
    ManageFormUseCase manageFormUseCase;
    @Mock
    ManageFormSectionUseCase manageFormSectionUseCase;
    @Mock
    ManageQuestionUseCase manageQuestionUseCase;
    @InjectMocks
    RecruitingInterviewAvailabilityFormProvisioner sut;

    @Test
    @DisplayName("OPEN 검증이 요구하는 비익명·SCHEDULE 필수 질문 1개 구조로 Form을 만들고 게시한다")
    void provisionPublishedAvailabilityForm() {
        given(manageFormUseCase.createDraft(any(CreateDraftFormCommand.class))).willReturn(500L);
        given(manageFormSectionUseCase.createSection(any(CreateFormSectionCommand.class))).willReturn(700L);
        given(manageQuestionUseCase.createQuestion(any(CreateQuestionCommand.class))).willReturn(600L);

        var mapping = sut.provision(interviewRound("본모집"), 99L);

        assertThat(mapping.formId()).isEqualTo(500L);
        assertThat(mapping.scheduleQuestionId()).isEqualTo(600L);

        ArgumentCaptor<CreateDraftFormCommand> formCaptor =
            ArgumentCaptor.forClass(CreateDraftFormCommand.class);
        then(manageFormUseCase).should().createDraft(formCaptor.capture());
        assertThat(formCaptor.getValue().isAnonymous()).isFalse();
        assertThat(formCaptor.getValue().title()).isEqualTo("본모집 면접 일정 조율");
        assertThat(formCaptor.getValue().createdMemberId()).isEqualTo(99L);

        ArgumentCaptor<CreateQuestionCommand> questionCaptor =
            ArgumentCaptor.forClass(CreateQuestionCommand.class);
        then(manageQuestionUseCase).should().createQuestion(questionCaptor.capture());
        assertThat(questionCaptor.getValue().sectionId()).isEqualTo(700L);
        assertThat(questionCaptor.getValue().type()).isEqualTo(QuestionType.SCHEDULE);
        assertThat(questionCaptor.getValue().isRequired()).isTrue();
        assertThat(questionCaptor.getValue().title()).isEqualTo("면접 가능한 시간을 선택해주세요");

        ArgumentCaptor<PublishFormCommand> publishCaptor =
            ArgumentCaptor.forClass(PublishFormCommand.class);
        then(manageFormUseCase).should().publishForm(publishCaptor.capture());
        assertThat(publishCaptor.getValue().formId()).isEqualTo(500L);
    }

    private RecruitingRound interviewRound(String title) {
        return RecruitingRound.createRegular(
            RecruitingSeason.create(1L, 2L),
            title,
            RecruitingRoundConfigurationCommand.of(
                List.of(ChallengerTrack.PLAN),
                false,
                Instant.parse("2026-08-01T00:00:00Z"),
                Instant.parse("2026-08-08T00:00:00Z"),
                Instant.parse("2026-08-10T00:00:00Z"),
                true,
                Instant.parse("2026-08-11T00:00:00Z"),
                Instant.parse("2026-08-14T00:00:00Z"),
                Instant.parse("2026-08-16T00:00:00Z"),
                null,
                null,
                null,
                null
            ).toDomain()
        );
    }
}
