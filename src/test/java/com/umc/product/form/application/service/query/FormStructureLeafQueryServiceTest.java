package com.umc.product.form.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

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

@DisplayName("Form 구조 leaf query service")
class FormStructureLeafQueryServiceTest {

    @Test
    @DisplayName("section query는 find/get/list를 info로 변환하고 get 부재를 거부한다")
    void section_query_contract() {
        LoadFormSectionPort port = mock(LoadFormSectionPort.class);
        FormSectionQueryService sut = new FormSectionQueryService(port);
        FormSection section = section();
        given(port.findById(10L)).willReturn(Optional.of(section));
        given(port.findById(404L)).willReturn(Optional.empty());
        given(port.listByFormId(1L)).willReturn(List.of(section));

        assertThat(sut.findById(10L)).get().satisfies(info -> {
            assertThat(info.sectionId()).isEqualTo(10L);
            assertThat(info.formId()).isEqualTo(1L);
        });
        assertThat(sut.getById(10L).title()).isEqualTo("섹션");
        assertThat(sut.listByFormId(1L)).singleElement()
            .extracting(info -> info.orderNo()).isEqualTo(1L);
        assertError(() -> sut.getById(404L), FormErrorCode.FORM_NOT_FOUND);
    }

    @Test
    @DisplayName("question query는 find/get/list를 info로 변환하고 get 부재를 거부한다")
    void question_query_contract() {
        LoadQuestionPort port = mock(LoadQuestionPort.class);
        QuestionQueryService sut = new QuestionQueryService(port);
        Question question = question();
        given(port.findById(20L)).willReturn(Optional.of(question));
        given(port.findById(404L)).willReturn(Optional.empty());
        given(port.listBySectionId(10L)).willReturn(List.of(question));

        assertThat(sut.findById(20L)).get().satisfies(info -> {
            assertThat(info.sectionId()).isEqualTo(10L);
            assertThat(info.type()).isEqualTo(QuestionType.RADIO);
        });
        assertThat(sut.getById(20L).title()).isEqualTo("질문");
        assertThat(sut.listBySectionId(10L)).singleElement()
            .extracting(info -> info.orderNo()).isEqualTo(1L);
        assertError(() -> sut.getById(404L), FormErrorCode.QUESTION_NOT_FOUND);
    }

    @Test
    @DisplayName("option query는 find/get/list를 info로 변환하고 get 부재를 거부한다")
    void option_query_contract() {
        LoadQuestionOptionPort port = mock(LoadQuestionOptionPort.class);
        QuestionOptionQueryService sut = new QuestionOptionQueryService(port);
        QuestionOption option = QuestionOption.create("보기", 1L, true, 11L);
        option.assignTo(question());
        ReflectionTestUtils.setField(option, "id", 30L);
        given(port.findById(30L)).willReturn(Optional.of(option));
        given(port.findById(404L)).willReturn(Optional.empty());
        given(port.listByQuestionId(20L)).willReturn(List.of(option));

        assertThat(sut.findById(30L)).get().satisfies(info -> {
            assertThat(info.questionId()).isEqualTo(20L);
            assertThat(info.nextSectionId()).isEqualTo(11L);
        });
        assertThat(sut.getById(30L).content()).isEqualTo("보기");
        assertThat(sut.listByQuestionId(20L)).singleElement()
            .extracting(info -> info.isOther()).isEqualTo(true);
        assertError(() -> sut.getById(404L), FormErrorCode.QUESTION_OPTION_NOT_FOUND);
    }

    private FormSection section() {
        Form form = Form.createDraft("폼", 1L);
        ReflectionTestUtils.setField(form, "id", 1L);
        FormSection section = FormSection.create(form, "섹션", "설명", 1L);
        ReflectionTestUtils.setField(section, "id", 10L);
        return section;
    }

    private Question question() {
        Question question = Question.create("질문", "설명", QuestionType.RADIO, true, 1L);
        question.assignTo(section());
        ReflectionTestUtils.setField(question, "id", 20L);
        return question;
    }

    private void assertError(Runnable action, FormErrorCode code) {
        assertThatThrownBy(action::run)
            .isInstanceOfSatisfying(FormDomainException.class, exception ->
                assertThat(exception.getBaseCode()).isEqualTo(code));
    }
}
