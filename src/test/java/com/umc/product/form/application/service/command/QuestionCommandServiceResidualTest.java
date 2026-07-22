package com.umc.product.form.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.form.application.port.in.command.dto.CreateQuestionCommand;
import com.umc.product.form.application.port.in.command.dto.DeleteQuestionCommand;
import com.umc.product.form.application.port.in.command.dto.ForkQuestionCommand;
import com.umc.product.form.application.port.in.command.dto.ReorderQuestionsCommand;
import com.umc.product.form.application.port.in.command.dto.UpdateQuestionCommand;
import com.umc.product.form.application.port.out.LoadFormSectionPort;
import com.umc.product.form.application.port.out.LoadQuestionPort;
import com.umc.product.form.application.port.out.SaveAnswerPort;
import com.umc.product.form.application.port.out.SaveQuestionOptionPort;
import com.umc.product.form.application.port.out.SaveQuestionPort;
import com.umc.product.form.domain.Form;
import com.umc.product.form.domain.FormSection;
import com.umc.product.form.domain.Question;
import com.umc.product.form.domain.enums.QuestionType;
import com.umc.product.form.domain.exception.FormDomainException;
import com.umc.product.form.domain.exception.FormErrorCode;

@ExtendWith(MockitoExtension.class)
@DisplayName("QuestionCommandService 잔여 경계")
class QuestionCommandServiceResidualTest {

    @Mock LoadFormSectionPort loadFormSectionPort;
    @Mock LoadQuestionPort loadQuestionPort;
    @Mock SaveQuestionPort saveQuestionPort;
    @Mock SaveQuestionOptionPort saveQuestionOptionPort;
    @Mock SaveAnswerPort saveAnswerPort;

    QuestionCommandService sut;

    @BeforeEach
    void setUp() {
        sut = new QuestionCommandService(
            loadFormSectionPort, loadQuestionPort, saveQuestionPort,
            saveQuestionOptionPort, saveAnswerPort
        );
    }

    @Test
    @DisplayName("기존 최대 순서 다음에 질문을 생성하고 없는 섹션은 거부한다")
    void creates_after_max_order_or_rejects_missing_section() {
        FormSection section = section();
        Question existing = question(10L, QuestionType.SHORT_TEXT, 4L);
        given(loadFormSectionPort.findById(1L)).willReturn(Optional.of(section));
        given(loadQuestionPort.listBySectionId(1L)).willReturn(List.of(existing));
        given(saveQuestionPort.save(org.mockito.ArgumentMatchers.any())).willAnswer(invocation -> {
            Question saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 20L);
            return saved;
        });

        Long id = sut.createQuestion(CreateQuestionCommand.builder()
            .sectionId(1L).title("신규").description("설명")
            .type(QuestionType.LONG_TEXT).isRequired(true).build());

        assertThat(id).isEqualTo(20L);
        then(saveQuestionPort).should().save(org.mockito.ArgumentMatchers.argThat(
            value -> value.getOrderNo() == 5L && value.getFormSection() == section));

        given(loadFormSectionPort.findById(404L)).willReturn(Optional.empty());
        assertError(() -> sut.createQuestion(CreateQuestionCommand.builder()
            .sectionId(404L).title("없음").type(QuestionType.SHORT_TEXT).isRequired(false).build()),
            FormErrorCode.FORM_NOT_FOUND);
    }

    @Test
    @DisplayName("질문 PATCH는 같은 타입은 유지하고 객관식에서 주관식으로 바뀔 때만 옵션을 정리한다")
    void update_type_change_cleanup_matrix() {
        Question objective = question(10L, QuestionType.RADIO, 1L);
        given(loadQuestionPort.findById(10L)).willReturn(Optional.of(objective));
        sut.updateQuestion(UpdateQuestionCommand.builder()
            .questionId(10L).type(QuestionType.SHORT_TEXT).title("변경")
            .clearDescription(true).isRequired(false).build());
        assertThat(objective.getType()).isEqualTo(QuestionType.SHORT_TEXT);
        then(saveQuestionOptionPort).should().deleteAllByQuestionId(10L);

        Question nonObjective = question(20L, QuestionType.FILE, 2L);
        given(loadQuestionPort.findById(20L)).willReturn(Optional.of(nonObjective));
        sut.updateQuestion(UpdateQuestionCommand.builder()
            .questionId(20L).type(QuestionType.CHECKBOX).build());
        assertThat(nonObjective.getType()).isEqualTo(QuestionType.CHECKBOX);

        Question sameType = question(30L, QuestionType.DROPDOWN, 3L);
        given(loadQuestionPort.findById(30L)).willReturn(Optional.of(sameType));
        sut.updateQuestion(UpdateQuestionCommand.builder()
            .questionId(30L).type(QuestionType.DROPDOWN).build());
        assertThat(sameType.getType()).isEqualTo(QuestionType.DROPDOWN);
        then(saveQuestionOptionPort).shouldHaveNoMoreInteractions();

        given(loadQuestionPort.findById(404L)).willReturn(Optional.empty());
        assertError(() -> sut.updateQuestion(
            UpdateQuestionCommand.builder().questionId(404L).build()), FormErrorCode.QUESTION_NOT_FOUND);
    }

    @Test
    @DisplayName("질문 삭제는 답변→선택지→질문 순서로 cascade한다")
    void deletes_dependencies_before_question() {
        sut.deleteQuestion(DeleteQuestionCommand.builder().questionId(10L).build());

        var order = inOrder(saveAnswerPort, saveQuestionOptionPort, saveQuestionPort);
        order.verify(saveAnswerPort).deleteByQuestionId(10L);
        order.verify(saveQuestionOptionPort).deleteAllByQuestionId(10L);
        order.verify(saveQuestionPort).deleteById(10L);
    }

    @Test
    @DisplayName("질문 재배치는 전체 ID를 한 번씩 제공한 경우에만 순서를 저장한다")
    void reorder_requires_exact_unique_ids() {
        Question first = question(10L, QuestionType.SHORT_TEXT, 1L);
        Question second = question(20L, QuestionType.LONG_TEXT, 2L);
        given(loadQuestionPort.listBySectionId(1L)).willReturn(List.of(first, second));

        sut.reorderQuestions(ReorderQuestionsCommand.builder()
            .sectionId(1L).orderedQuestionIds(List.of(20L, 10L)).build());
        assertThat(second.getOrderNo()).isEqualTo(1L);
        assertThat(first.getOrderNo()).isEqualTo(2L);
        then(saveQuestionPort).should().saveAll(List.of(first, second));

        for (List<Long> invalid : List.of(List.of(10L), List.of(10L, 10L, 20L), List.of(10L, 30L))) {
            assertError(() -> sut.reorderQuestions(ReorderQuestionsCommand.builder()
                .sectionId(1L).orderedQuestionIds(invalid).build()), FormErrorCode.INVALID_VOTE_FORM_STRUCTURE);
        }
    }

    @Test
    @DisplayName("질문 비활성화는 저장하고 없는 질문은 거부한다")
    void deactivates_or_rejects_missing_question() {
        Question question = question(10L, QuestionType.SHORT_TEXT, 1L);
        given(loadQuestionPort.findById(10L)).willReturn(Optional.of(question));
        sut.deactivateQuestion(10L);
        assertThat(question.isActive()).isFalse();
        then(saveQuestionPort).should().save(question);

        given(loadQuestionPort.findById(404L)).willReturn(Optional.empty());
        assertError(() -> sut.deactivateQuestion(404L), FormErrorCode.QUESTION_NOT_FOUND);
    }

    @Test
    @DisplayName("fork는 원본 snapshot을 같은 섹션에 저장하고 원본을 비활성화한다")
    void forks_and_deactivates_origin() {
        Question origin = question(10L, QuestionType.PORTFOLIO, 1L);
        origin.assignTo(section());
        given(loadQuestionPort.findById(10L)).willReturn(Optional.of(origin));
        given(saveQuestionPort.save(org.mockito.ArgumentMatchers.any())).willAnswer(invocation -> {
            Question value = invocation.getArgument(0);
            if (value.getId() == null) ReflectionTestUtils.setField(value, "id", 20L);
            return value;
        });

        Long id = sut.forkQuestion(ForkQuestionCommand.builder().originQuestionId(10L).build());

        assertThat(id).isEqualTo(20L);
        assertThat(origin.isActive()).isFalse();
        then(saveQuestionPort).should(org.mockito.Mockito.times(2)).save(org.mockito.ArgumentMatchers.any());

        given(loadQuestionPort.findById(404L)).willReturn(Optional.empty());
        assertError(() -> sut.forkQuestion(
            ForkQuestionCommand.builder().originQuestionId(404L).build()), FormErrorCode.QUESTION_NOT_FOUND);
    }

    private FormSection section() {
        Form form = Form.createDraft("폼", 1L);
        ReflectionTestUtils.setField(form, "id", 100L);
        FormSection section = FormSection.create(form, "섹션", "설명", 1L);
        ReflectionTestUtils.setField(section, "id", 1L);
        return section;
    }

    private Question question(Long id, QuestionType type, Long orderNo) {
        Question question = Question.create("질문", "설명", type, true, orderNo);
        ReflectionTestUtils.setField(question, "id", id);
        return question;
    }

    private void assertError(Runnable action, FormErrorCode code) {
        assertThatThrownBy(action::run)
            .isInstanceOfSatisfying(FormDomainException.class, exception ->
                assertThat(exception.getBaseCode()).isEqualTo(code));
    }
}
