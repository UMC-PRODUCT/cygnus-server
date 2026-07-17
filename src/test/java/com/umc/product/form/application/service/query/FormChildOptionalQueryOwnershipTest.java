package com.umc.product.form.application.service.query;

import static com.umc.product.form.application.service.FormAccessTestFixtures.actor;
import static com.umc.product.form.application.service.FormAccessTestFixtures.owner;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

import com.umc.product.form.application.port.out.LoadFormSectionPort;
import com.umc.product.form.application.port.out.LoadQuestionOptionPort;
import com.umc.product.form.application.port.out.LoadQuestionPort;
import com.umc.product.form.application.service.FormOwnershipAccessService;
import com.umc.product.form.domain.Form;
import com.umc.product.form.domain.FormOperation;
import com.umc.product.form.domain.FormSection;
import com.umc.product.form.domain.Question;
import com.umc.product.form.domain.QuestionOption;
import com.umc.product.form.domain.enums.QuestionType;
import com.umc.product.form.domain.exception.FormDomainException;
import com.umc.product.form.domain.exception.FormErrorCode;

@ExtendWith(MockitoExtension.class)
class FormChildOptionalQueryOwnershipTest {

    private static final Long FORM_ID = 100L;
    private static final Long MEMBER_ID = 200L;

    @Mock
    LoadFormSectionPort loadFormSectionPort;
    @Mock
    LoadQuestionPort loadQuestionPort;
    @Mock
    LoadQuestionOptionPort loadQuestionOptionPort;
    @Mock
    FormOwnershipAccessService ownershipAccessService;

    private FormSectionQueryService formSectionQueryService;
    private QuestionQueryService questionQueryService;
    private QuestionOptionQueryService questionOptionQueryService;

    @BeforeEach
    void setUp() {
        formSectionQueryService = new FormSectionQueryService(
            loadFormSectionPort, ownershipAccessService
        );
        questionQueryService = new QuestionQueryService(
            loadQuestionPort, loadFormSectionPort, ownershipAccessService
        );
        questionOptionQueryService = new QuestionOptionQueryService(
            loadQuestionOptionPort, loadQuestionPort, ownershipAccessService
        );
    }

    @Test
    @DisplayName("missing section Optional도 expected owner policy를 먼저 검증한다")
    void missing_section도_expected_owner를_검증한다() {
        // given
        given(loadFormSectionPort.findById(1L)).willReturn(Optional.empty());

        // when
        var result = formSectionQueryService.findById(owner(FORM_ID), actor(MEMBER_ID), 1L);

        // then
        assertThat(result).isEmpty();
        then(ownershipAccessService).should().requireRead(
            FORM_ID, owner(FORM_ID), actor(MEMBER_ID), FormOperation.READ
        );
    }

    @Test
    @DisplayName("missing question Optional도 expected owner policy를 먼저 검증한다")
    void missing_question도_expected_owner를_검증한다() {
        // given
        given(loadQuestionPort.findById(2L)).willReturn(Optional.empty());

        // when
        var result = questionQueryService.findById(owner(FORM_ID), actor(MEMBER_ID), 2L);

        // then
        assertThat(result).isEmpty();
        then(ownershipAccessService).should().requireRead(
            FORM_ID, owner(FORM_ID), actor(MEMBER_ID), FormOperation.READ
        );
    }

    @Test
    @DisplayName("missing option Optional도 expected owner policy를 먼저 검증한다")
    void missing_option도_expected_owner를_검증한다() {
        // given
        given(loadQuestionOptionPort.findById(3L)).willReturn(Optional.empty());

        // when
        var result = questionOptionQueryService.findById(owner(FORM_ID), actor(MEMBER_ID), 3L);

        // then
        assertThat(result).isEmpty();
        then(ownershipAccessService).should().requireRead(
            FORM_ID, owner(FORM_ID), actor(MEMBER_ID), FormOperation.READ
        );
    }

    @Test
    @DisplayName("existing foreign question Optional은 실제 parent root 검증에서 거부한다")
    void existing_foreign_question은_실제_parent_root에서_거부한다() {
        // given
        Question foreign = question(2L, 999L);
        given(loadQuestionPort.findById(2L)).willReturn(Optional.of(foreign));
        // when / then
        assertThatThrownBy(() -> questionQueryService.findById(
            owner(FORM_ID), actor(MEMBER_ID), 2L
        ))
            .isInstanceOf(FormDomainException.class)
            .hasFieldOrPropertyWithValue("baseCode", FormErrorCode.FORM_OWNERSHIP_FORBIDDEN);
    }

    @Test
    @DisplayName("section 목록의 expected owner 밖 child는 fail closed한다")
    void section_목록의_scope_밖_child는_fail_closed한다() {
        // given
        given(loadFormSectionPort.listByFormId(FORM_ID))
            .willReturn(List.of(section(1L, 999L)));

        // when / then
        assertOwnershipDenied(() -> formSectionQueryService.listByFormId(
            owner(FORM_ID), actor(MEMBER_ID)
        ));
    }

    @Test
    @DisplayName("question 목록의 expected owner 밖 child는 fail closed한다")
    void question_목록의_scope_밖_child는_fail_closed한다() {
        // given
        given(loadFormSectionPort.findById(1L)).willReturn(Optional.of(section(1L, FORM_ID)));
        given(loadQuestionPort.listBySectionId(1L)).willReturn(List.of(question(2L, 999L)));

        // when / then
        assertOwnershipDenied(() -> questionQueryService.listBySectionId(
            owner(FORM_ID), actor(MEMBER_ID), 1L
        ));
    }

    @Test
    @DisplayName("option 목록의 expected owner 밖 child는 fail closed한다")
    void option_목록의_scope_밖_child는_fail_closed한다() {
        // given
        given(loadQuestionPort.findById(2L)).willReturn(Optional.of(question(2L, FORM_ID)));
        given(loadQuestionOptionPort.listByQuestionId(2L))
            .willReturn(List.of(option(3L, 999L)));

        // when / then
        assertOwnershipDenied(() -> questionOptionQueryService.listByQuestionId(
            owner(FORM_ID), actor(MEMBER_ID), 2L
        ));
    }

    private static FormSection section(Long sectionId, Long formId) {
        FormSection section = FormSection.create(form(formId), "섹션", null, 1L);
        ReflectionTestUtils.setField(section, "id", sectionId);
        return section;
    }

    private static Question question(Long questionId, Long formId) {
        FormSection section = section(1L, formId);
        Question question = Question.create("질문", QuestionType.SHORT_TEXT, false, 1L);
        question.assignTo(section);
        ReflectionTestUtils.setField(question, "id", questionId);
        return question;
    }

    private static QuestionOption option(Long optionId, Long formId) {
        QuestionOption option = QuestionOption.create("선택", 1L, false, null);
        option.assignTo(question(2L, formId));
        ReflectionTestUtils.setField(option, "id", optionId);
        return option;
    }

    private static Form form(Long formId) {
        Form form = Form.createDraft("폼", MEMBER_ID);
        ReflectionTestUtils.setField(form, "id", formId);
        return form;
    }

    private static void assertOwnershipDenied(
        org.assertj.core.api.ThrowableAssert.ThrowingCallable callable
    ) {
        assertThatThrownBy(callable)
            .isInstanceOf(FormDomainException.class)
            .hasFieldOrPropertyWithValue("baseCode", FormErrorCode.FORM_OWNERSHIP_FORBIDDEN);
    }
}
