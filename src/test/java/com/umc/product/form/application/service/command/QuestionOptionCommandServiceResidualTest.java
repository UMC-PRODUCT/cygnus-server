package com.umc.product.form.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.form.application.port.in.command.dto.CreateQuestionOptionCommand;
import com.umc.product.form.application.port.in.command.dto.DeleteQuestionOptionCommand;
import com.umc.product.form.application.port.in.command.dto.ReorderQuestionOptionsCommand;
import com.umc.product.form.application.port.in.command.dto.UpdateQuestionOptionCommand;
import com.umc.product.form.application.port.out.LoadFormSectionPort;
import com.umc.product.form.application.port.out.LoadQuestionOptionPort;
import com.umc.product.form.application.port.out.LoadQuestionPort;
import com.umc.product.form.application.port.out.SaveQuestionOptionPort;
import com.umc.product.form.domain.Form;
import com.umc.product.form.domain.FormSection;
import com.umc.product.form.domain.Question;
import com.umc.product.form.domain.QuestionOption;
import com.umc.product.form.domain.enums.QuestionType;
import com.umc.product.form.domain.exception.FormDomainException;
import com.umc.product.form.domain.exception.FormErrorCode;

@ExtendWith(MockitoExtension.class)
@DisplayName("QuestionOptionCommandService 잔여 경계")
class QuestionOptionCommandServiceResidualTest {

    @Mock LoadFormSectionPort loadFormSectionPort;
    @Mock LoadQuestionPort loadQuestionPort;
    @Mock LoadQuestionOptionPort loadQuestionOptionPort;
    @Mock SaveQuestionOptionPort saveQuestionOptionPort;

    QuestionOptionCommandService sut;

    @BeforeEach
    void setUp() {
        sut = new QuestionOptionCommandService(
            loadFormSectionPort, loadQuestionPort, loadQuestionOptionPort, saveQuestionOptionPort
        );
    }

    @Test
    @DisplayName("선택지는 기존 최대 순서 다음에 생성되며 next section이 없으면 추가 검증을 생략한다")
    void creates_after_max_order_without_branch_target() {
        Question question = question(10L, QuestionType.CHECKBOX, section(1L, form(100L)));
        QuestionOption existing = option(20L, question, 4L, null);
        given(loadQuestionPort.findById(10L)).willReturn(Optional.of(question));
        given(loadQuestionOptionPort.listByQuestionId(10L)).willReturn(List.of(existing));
        given(saveQuestionOptionPort.save(any())).willAnswer(invocation -> {
            QuestionOption saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 30L);
            return saved;
        });

        Long id = sut.createOption(CreateQuestionOptionCommand.builder()
            .questionId(10L).content("보기").isOther(false).build());

        assertThat(id).isEqualTo(30L);
        then(saveQuestionOptionPort).should().save(org.mockito.ArgumentMatchers.argThat(
            value -> value.getOrderNo() == 5L && value.getQuestion() == question));
    }

    @Test
    @DisplayName("없는 질문과 비객관식 질문의 next section은 거부한다")
    void rejects_missing_question_and_unsupported_type() {
        given(loadQuestionPort.findById(404L)).willReturn(Optional.empty());
        assertError(() -> sut.createOption(CreateQuestionOptionCommand.builder()
            .questionId(404L).content("보기").build()), FormErrorCode.FORM_NOT_FOUND);

        Question text = question(10L, QuestionType.SHORT_TEXT, section(1L, form(100L)));
        given(loadQuestionPort.findById(10L)).willReturn(Optional.of(text));
        given(loadQuestionOptionPort.listByQuestionId(10L)).willReturn(List.of());
        assertError(() -> sut.createOption(CreateQuestionOptionCommand.builder()
            .questionId(10L).content("보기").nextSectionId(2L).build()),
            FormErrorCode.INVALID_VOTE_FORM_STRUCTURE);
    }

    @Test
    @DisplayName("next section은 존재하고 같은 폼에 속해야 하며 DROPDOWN도 허용한다")
    void validates_target_exists_and_same_form() {
        Form form = form(100L);
        FormSection current = section(1L, form);
        Question dropdown = question(10L, QuestionType.DROPDOWN, current);
        given(loadQuestionPort.findById(10L)).willReturn(Optional.of(dropdown));
        given(loadQuestionOptionPort.listByQuestionId(10L)).willReturn(List.of());

        given(loadFormSectionPort.findById(404L)).willReturn(Optional.empty());
        assertError(() -> sut.createOption(CreateQuestionOptionCommand.builder()
            .questionId(10L).content("보기").nextSectionId(404L).build()),
            FormErrorCode.INVALID_VOTE_FORM_STRUCTURE);

        given(loadFormSectionPort.findById(2L)).willReturn(Optional.of(section(2L, form(200L))));
        assertError(() -> sut.createOption(CreateQuestionOptionCommand.builder()
            .questionId(10L).content("보기").nextSectionId(2L).build()),
            FormErrorCode.INVALID_VOTE_FORM_STRUCTURE);

        given(loadFormSectionPort.findById(3L)).willReturn(Optional.of(section(3L, form)));
        given(saveQuestionOptionPort.save(any())).willAnswer(invocation -> invocation.getArgument(0));
        sut.createOption(CreateQuestionOptionCommand.builder()
            .questionId(10L).content("보기").nextSectionId(3L).build());
        then(saveQuestionOptionPort).should().save(any());
    }

    @Test
    @DisplayName("선택지 PATCH는 clear next section과 일반 값 변경을 구분하고 없는 선택지를 거부한다")
    void updates_clears_or_rejects_missing_option() {
        Form form = form(100L);
        Question question = question(10L, QuestionType.RADIO, section(1L, form));
        QuestionOption option = option(20L, question, 1L, 3L);
        given(loadQuestionOptionPort.findById(20L)).willReturn(Optional.of(option));

        sut.updateOption(UpdateQuestionOptionCommand.builder()
            .optionId(20L).content("변경").isOther(true).clearNextSectionId(true).build());
        assertThat(option.getContent()).isEqualTo("변경");
        assertThat(option.isOther()).isTrue();
        assertThat(option.getNextSectionId()).isNull();
        then(saveQuestionOptionPort).should().save(option);

        given(loadFormSectionPort.findById(2L)).willReturn(Optional.of(section(2L, form)));
        sut.updateOption(UpdateQuestionOptionCommand.builder()
            .optionId(20L).nextSectionId(2L).build());
        assertThat(option.getNextSectionId()).isEqualTo(2L);

        given(loadQuestionOptionPort.findById(404L)).willReturn(Optional.empty());
        assertError(() -> sut.updateOption(
            UpdateQuestionOptionCommand.builder().optionId(404L).build()), FormErrorCode.FORM_NOT_FOUND);
    }

    @Test
    @DisplayName("선택지 삭제는 ID를 그대로 위임한다")
    void deletes_by_id() {
        sut.deleteOption(DeleteQuestionOptionCommand.builder().optionId(20L).build());
        then(saveQuestionOptionPort).should().deleteById(20L);
    }

    @Test
    @DisplayName("선택지 재배치는 전체 ID를 한 번씩 제공한 경우에만 저장한다")
    void reorder_requires_exact_unique_ids() {
        Question question = question(10L, QuestionType.RADIO, section(1L, form(100L)));
        QuestionOption first = option(20L, question, 1L, null);
        QuestionOption second = option(30L, question, 2L, null);
        given(loadQuestionOptionPort.listByQuestionId(10L)).willReturn(List.of(first, second));

        sut.reorderOptions(ReorderQuestionOptionsCommand.builder()
            .questionId(10L).orderedOptionIds(List.of(30L, 20L)).build());
        assertThat(second.getOrderNo()).isEqualTo(1L);
        assertThat(first.getOrderNo()).isEqualTo(2L);
        then(saveQuestionOptionPort).should().saveAll(List.of(first, second));

        for (List<Long> invalid : List.of(List.of(20L), List.of(20L, 20L, 30L), List.of(20L, 40L))) {
            assertError(() -> sut.reorderOptions(ReorderQuestionOptionsCommand.builder()
                .questionId(10L).orderedOptionIds(invalid).build()), FormErrorCode.INVALID_VOTE_FORM_STRUCTURE);
        }
    }

    private Form form(Long id) {
        Form form = Form.createDraft("폼", 1L);
        ReflectionTestUtils.setField(form, "id", id);
        return form;
    }

    private FormSection section(Long id, Form form) {
        FormSection section = FormSection.create(form, "섹션", null, id);
        ReflectionTestUtils.setField(section, "id", id);
        return section;
    }

    private Question question(Long id, QuestionType type, FormSection section) {
        Question question = Question.create("질문", type, true, 1L);
        question.assignTo(section);
        ReflectionTestUtils.setField(question, "id", id);
        return question;
    }

    private QuestionOption option(Long id, Question question, Long orderNo, Long nextSectionId) {
        QuestionOption option = QuestionOption.create("보기", orderNo, false, nextSectionId);
        option.assignTo(question);
        ReflectionTestUtils.setField(option, "id", id);
        return option;
    }

    private void assertError(Runnable action, FormErrorCode code) {
        assertThatThrownBy(action::run)
            .isInstanceOfSatisfying(FormDomainException.class, exception ->
                assertThat(exception.getBaseCode()).isEqualTo(code));
    }
}
