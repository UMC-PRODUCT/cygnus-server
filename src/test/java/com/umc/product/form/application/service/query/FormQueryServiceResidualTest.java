package com.umc.product.form.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.form.application.port.out.LoadFormPort;
import com.umc.product.form.application.port.out.LoadFormSectionPort;
import com.umc.product.form.application.port.out.LoadQuestionOptionPort;
import com.umc.product.form.application.port.out.LoadQuestionPort;
import com.umc.product.form.domain.Form;
import com.umc.product.form.domain.FormSection;
import com.umc.product.form.domain.Question;
import com.umc.product.form.domain.QuestionOption;
import com.umc.product.form.domain.enums.QuestionType;
import com.umc.product.form.domain.exception.FormDomainException;
import com.umc.product.form.domain.exception.FormErrorCode;

@DisplayName("FormQueryService batch·not-found 경계")
class FormQueryServiceResidualTest {

    LoadFormPort forms;
    LoadFormSectionPort sections;
    LoadQuestionPort questions;
    LoadQuestionOptionPort options;
    FormQueryService sut;

    @BeforeEach
    void setUp() {
        forms = mock(LoadFormPort.class);
        sections = mock(LoadFormSectionPort.class);
        questions = mock(LoadQuestionPort.class);
        options = mock(LoadQuestionOptionPort.class);
        sut = new FormQueryService(forms, sections, questions, options);
    }

    @Test
    @DisplayName("find/get은 FormInfo로 변환하고 get 부재를 FORM_NOT_FOUND로 변환한다")
    void find_get_contract() {
        Form form = form(1L, "폼");
        given(forms.findById(1L)).willReturn(Optional.of(form));
        given(forms.findById(404L)).willReturn(Optional.empty());

        assertThat(sut.findById(1L)).get().satisfies(info -> {
            assertThat(info.id()).isEqualTo(1L);
            assertThat(info.title()).isEqualTo("폼");
        });
        assertThat(sut.findById(404L)).isEmpty();
        assertThat(sut.getById(1L).id()).isEqualTo(1L);
        assertError(() -> sut.getById(404L));
    }

    @Test
    @DisplayName("batch null·empty는 모든 port 호출 없이 빈 map을 반환한다")
    void batch_empty_short_circuit() {
        assertThat(sut.batchGetFormsWithStructure(null)).isEmpty();
        assertThat(sut.batchGetFormsWithStructure(List.of())).isEmpty();
        then(forms).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("batch는 중복 form ID를 입력 순서로 제거하고 부분 구조를 각 폼에 조립한다")
    void batch_deduplicates_and_assembles_partial_structures() {
        Form first = form(1L, "첫 폼");
        Form duplicateFirst = form(1L, "중복 폼");
        Form second = form(2L, "둘째 폼");
        FormSection section = section(10L, first);
        Question question = question(20L, section);
        QuestionOption option = option(30L, question);
        given(forms.batchGetByIds(List.of(2L, 1L))).willReturn(List.of(second, first, duplicateFirst));
        given(sections.listByFormIds(List.of(2L, 1L))).willReturn(List.of(section));
        given(questions.listBySectionIdIn(Set.of(10L))).willReturn(List.of(question));
        given(options.listByQuestionIdIn(Set.of(20L))).willReturn(List.of(option));

        var result = sut.batchGetFormsWithStructure(List.of(2L, 1L, 2L));

        assertThat(result.keySet()).containsExactly(2L, 1L);
        assertThat(result.get(2L).sections()).isEmpty();
        assertThat(result.get(1L).sections()).singleElement().satisfies(sectionInfo ->
            assertThat(sectionInfo.questions()).singleElement().satisfies(questionInfo ->
                assertThat(questionInfo.options()).singleElement()
                    .extracting(value -> value.optionId()).isEqualTo(30L)));
        assertThat(result.get(1L).title()).isEqualTo("첫 폼");
    }

    @Test
    @DisplayName("구조 조회와 question 범위 구조 조회는 없는 폼을 동일하게 거부한다")
    void structure_not_found_paths() {
        given(forms.findById(404L)).willReturn(Optional.empty());

        assertError(() -> sut.getFormWithStructure(404L));
        assertError(() -> sut.getFormWithStructureByQuestionIds(404L, Set.of()));
    }

    private Form form(Long id, String title) {
        Form form = Form.createDraft(title, 1L, "설명", false);
        ReflectionTestUtils.setField(form, "id", id);
        return form;
    }

    private FormSection section(Long id, Form form) {
        FormSection section = FormSection.create(form, "섹션", "설명", 1L);
        ReflectionTestUtils.setField(section, "id", id);
        return section;
    }

    private Question question(Long id, FormSection section) {
        Question question = Question.create("질문", QuestionType.RADIO, true, 1L);
        question.assignTo(section);
        ReflectionTestUtils.setField(question, "id", id);
        return question;
    }

    private QuestionOption option(Long id, Question question) {
        QuestionOption option = QuestionOption.create("보기", 1L, false, null);
        option.assignTo(question);
        ReflectionTestUtils.setField(option, "id", id);
        return option;
    }

    private void assertError(Runnable action) {
        assertThatThrownBy(action::run)
            .isInstanceOfSatisfying(FormDomainException.class, exception ->
                assertThat(exception.getBaseCode()).isEqualTo(FormErrorCode.FORM_NOT_FOUND));
    }
}
