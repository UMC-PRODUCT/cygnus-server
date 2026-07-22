package com.umc.product.form.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.form.application.port.in.command.ManageFormUseCase;
import com.umc.product.form.application.port.in.command.dto.CreateVoteCommand;
import com.umc.product.form.application.port.in.command.dto.DeleteFormCommand;
import com.umc.product.form.application.port.out.SaveFormPort;
import com.umc.product.form.application.port.out.SaveFormSectionPort;
import com.umc.product.form.application.port.out.SaveQuestionOptionPort;
import com.umc.product.form.application.port.out.SaveQuestionPort;
import com.umc.product.form.domain.QuestionOption;
import com.umc.product.form.domain.enums.FormStatus;
import com.umc.product.form.domain.enums.QuestionType;

@DisplayName("VoteService")
class VoteServiceTest {

    @Test
    @DisplayName("복수 선택 투표는 PUBLISHED Form·단일 section·CHECKBOX question·순서화된 option을 생성한다")
    void creates_multiple_choice_vote_structure() {
        SaveFormPort forms = mock(SaveFormPort.class);
        SaveFormSectionPort sections = mock(SaveFormSectionPort.class);
        SaveQuestionPort questions = mock(SaveQuestionPort.class);
        SaveQuestionOptionPort options = mock(SaveQuestionOptionPort.class);
        ManageFormUseCase manage = mock(ManageFormUseCase.class);
        VoteService sut = new VoteService(forms, sections, questions, options, manage);
        given(forms.save(any())).willAnswer(invocation -> withId(invocation.getArgument(0), 1L));
        given(sections.save(any())).willAnswer(invocation -> withId(invocation.getArgument(0), 2L));
        given(questions.save(any())).willAnswer(invocation -> withId(invocation.getArgument(0), 3L));

        Long id = sut.createVote(CreateVoteCommand.builder()
            .createdMemberId(9L).title("투표").isAnonymous(true).allowMultipleChoice(true)
            .options(List.of("A", "B")).build());

        assertThat(id).isEqualTo(1L);
        then(forms).should().save(org.mockito.ArgumentMatchers.argThat(
            value -> value.getStatus() == FormStatus.PUBLISHED && value.isAnonymous()));
        then(sections).should().save(org.mockito.ArgumentMatchers.argThat(
            value -> value.getForm().getId().equals(1L) && value.getOrderNo() == 1L));
        then(questions).should().save(org.mockito.ArgumentMatchers.argThat(
            value -> value.getType() == QuestionType.CHECKBOX && value.getFormSection().getId().equals(2L)));
        ArgumentCaptor<List<QuestionOption>> captor = ArgumentCaptor.forClass(List.class);
        then(options).should().saveAll(captor.capture());
        assertThat(captor.getValue()).extracting(QuestionOption::getContent).containsExactly("A", "B");
        assertThat(captor.getValue()).extracting(QuestionOption::getOrderNo).containsExactly(1L, 2L);
        assertThat(captor.getValue()).allSatisfy(option ->
            assertThat(option.getQuestion().getId()).isEqualTo(3L));
    }

    @Test
    @DisplayName("단일 선택과 빈 option 투표는 RADIO 및 빈 저장 목록으로 생성한다")
    void creates_single_choice_vote_with_empty_options() {
        SaveFormPort forms = mock(SaveFormPort.class);
        SaveFormSectionPort sections = mock(SaveFormSectionPort.class);
        SaveQuestionPort questions = mock(SaveQuestionPort.class);
        SaveQuestionOptionPort options = mock(SaveQuestionOptionPort.class);
        VoteService sut = new VoteService(forms, sections, questions, options, mock(ManageFormUseCase.class));
        given(forms.save(any())).willAnswer(invocation -> withId(invocation.getArgument(0), 1L));
        given(sections.save(any())).willAnswer(invocation -> withId(invocation.getArgument(0), 2L));
        given(questions.save(any())).willAnswer(invocation -> withId(invocation.getArgument(0), 3L));

        sut.createVote(CreateVoteCommand.builder()
            .createdMemberId(9L).title("투표").allowMultipleChoice(false).options(List.of()).build());

        then(questions).should().save(org.mockito.ArgumentMatchers.argThat(
            value -> value.getType() == QuestionType.RADIO));
        then(options).should().saveAll(List.of());
    }

    @Test
    @DisplayName("투표 삭제는 form 전체 cascade 삭제 command로 위임한다")
    void delete_delegates_to_form_cascade() {
        ManageFormUseCase manage = mock(ManageFormUseCase.class);
        VoteService sut = new VoteService(
            mock(SaveFormPort.class), mock(SaveFormSectionPort.class),
            mock(SaveQuestionPort.class), mock(SaveQuestionOptionPort.class), manage
        );

        sut.deleteVote(1L);

        then(manage).should().deleteForm(org.mockito.ArgumentMatchers.argThat(
            (DeleteFormCommand command) -> command.formId().equals(1L)));
    }

    private <T> T withId(T entity, Long id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }
}
