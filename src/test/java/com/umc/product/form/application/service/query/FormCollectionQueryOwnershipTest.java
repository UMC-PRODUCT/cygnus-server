package com.umc.product.form.application.service.query;

import static com.umc.product.form.application.service.FormAccessTestFixtures.actor;
import static com.umc.product.form.application.service.FormAccessTestFixtures.owner;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.authentication.application.service.SecureTokenGenerator;
import com.umc.product.form.application.port.in.query.GetAnswerUseCase;
import com.umc.product.form.application.port.out.LoadAnswerPort;
import com.umc.product.form.application.port.out.LoadFormPort;
import com.umc.product.form.application.port.out.LoadFormResponsePort;
import com.umc.product.form.application.port.out.LoadFormSectionPort;
import com.umc.product.form.application.port.out.LoadQuestionOptionPort;
import com.umc.product.form.application.port.out.LoadQuestionPort;
import com.umc.product.form.application.service.FormOwnershipAccessService;
import com.umc.product.form.domain.Answer;
import com.umc.product.form.domain.Form;
import com.umc.product.form.domain.FormOperation;
import com.umc.product.form.domain.FormOwnerReference;
import com.umc.product.form.domain.FormResponse;
import com.umc.product.form.domain.FormSection;
import com.umc.product.form.domain.Question;
import com.umc.product.form.domain.enums.QuestionType;
import com.umc.product.form.domain.exception.FormDomainException;
import com.umc.product.form.domain.exception.FormErrorCode;

@ExtendWith(MockitoExtension.class)
class FormCollectionQueryOwnershipTest {

    private static final Long FORM_ID = 100L;
    private static final Long FOREIGN_FORM_ID = 200L;
    private static final Long MEMBER_ID = 300L;

    @Mock
    LoadFormPort loadFormPort;
    @Mock
    LoadFormSectionPort loadFormSectionPort;
    @Mock
    LoadQuestionPort loadQuestionPort;
    @Mock
    LoadQuestionOptionPort loadQuestionOptionPort;
    @Mock
    LoadFormResponsePort loadFormResponsePort;
    @Mock
    LoadAnswerPort loadAnswerPort;
    @Mock
    GetAnswerUseCase getAnswerUseCase;
    @Mock
    FormOwnershipAccessService ownershipAccessService;
    @Mock
    SecureTokenGenerator secureTokenGenerator;

    private FormQueryService formQueryService;
    private FormResponseQueryService formResponseQueryService;
    private AnswerQueryService answerQueryService;

    @BeforeEach
    void setUp() {
        formQueryService = new FormQueryService(
            loadFormPort,
            loadFormSectionPort,
            loadQuestionPort,
            loadQuestionOptionPort,
            ownershipAccessService
        );
        formResponseQueryService = new FormResponseQueryService(
            loadFormResponsePort,
            getAnswerUseCase,
            ownershipAccessService,
            secureTokenGenerator
        );
        answerQueryService = new AnswerQueryService(
            loadAnswerPort,
            loadFormResponsePort,
            ownershipAccessService,
            secureTokenGenerator
        );
    }

    @Test
    @DisplayName("respondent draft 목록은 expected owner scope 밖 draft를 제외한다")
    void respondent_draft_목록은_expected_owner_scope_밖_draft를_제외한다() {
        // given
        FormResponse scoped = namedResponse(1L, FORM_ID);
        FormResponse foreign = namedResponse(2L, FOREIGN_FORM_ID);
        given(loadFormResponsePort.findAllDraftByRespondentMemberId(MEMBER_ID))
            .willReturn(List.of(scoped, foreign));

        // when
        var result = formResponseQueryService.listDraftByRespondent(
            List.of(owner(FORM_ID)), actor(MEMBER_ID)
        );

        // then
        assertThat(result).extracting("id").containsExactly(1L);
        then(ownershipAccessService).should(never()).requireRead(
            eq(FOREIGN_FORM_ID), any(), any(), any()
        );
    }

    @Test
    @DisplayName("respondent draft 목록은 빈 expected owner scope를 안전한 empty로 처리한다")
    void respondent_draft_목록은_빈_scope를_empty로_처리한다() {
        // given
        List<FormOwnerReference> emptyScope = List.of();

        // when
        var result = formResponseQueryService.listDraftByRespondent(
            emptyScope, actor(MEMBER_ID)
        );

        // then
        assertThat(result).isEmpty();
        then(loadFormResponsePort).shouldHaveNoInteractions();
        then(ownershipAccessService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("respondent draft 목록은 null expected owner scope를 안전한 empty로 처리한다")
    void respondent_draft_목록은_null_scope를_empty로_처리한다() {
        // given
        List<FormOwnerReference> nullScope = null;

        // when
        var result = formResponseQueryService.listDraftByRespondent(
            nullScope, actor(MEMBER_ID)
        );

        // then
        assertThat(result).isEmpty();
        then(loadFormResponsePort).shouldHaveNoInteractions();
        then(ownershipAccessService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("null expected owner 원소는 collection query를 fail closed한다")
    void null_expected_owner_원소는_fail_closed한다() {
        // given
        List<FormOwnerReference> invalidScope = Collections.singletonList(null);

        // when / then
        assertOwnershipDenied(() -> formResponseQueryService.listDraftByRespondent(
            invalidScope, actor(MEMBER_ID)
        ));
        then(loadFormResponsePort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("respondent draft scope ownership 또는 policy 실패는 조회 전에 fail closed한다")
    void respondent_draft_scope_policy_실패는_fail_closed한다() {
        // given
        willThrow(new FormDomainException(FormErrorCode.FORM_OWNERSHIP_FORBIDDEN))
            .given(ownershipAccessService)
            .requireRead(
                FORM_ID, owner(FORM_ID), actor(MEMBER_ID), FormOperation.READ
            );

        // when / then
        assertOwnershipDenied(() -> formResponseQueryService.listDraftByRespondent(
            List.of(owner(FORM_ID)), actor(MEMBER_ID)
        ));
        then(loadFormResponsePort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("같은 form ID의 expected owner가 중복되면 collection query를 fail closed한다")
    void 중복_form_ID_scope는_fail_closed한다() {
        // given
        FormOwnerReference duplicate = FormOwnerReference.of(
            FORM_ID, "form.other", FORM_ID.toString(), "default"
        );

        // when / then
        assertOwnershipDenied(() -> formResponseQueryService.listDraftByRespondent(
            List.of(owner(FORM_ID), duplicate), actor(MEMBER_ID)
        ));
        then(loadFormResponsePort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Form batch adapter가 owner scope 밖 root를 반환하면 fail closed한다")
    void form_batch의_scope_밖_root는_fail_closed한다() {
        // given
        given(loadFormPort.batchGetByIds(List.of(FORM_ID)))
            .willReturn(List.of(form(FOREIGN_FORM_ID)));

        // when / then
        assertOwnershipDenied(() -> formQueryService.batchGetFormsWithStructure(
            List.of(owner(FORM_ID)), actor(MEMBER_ID)
        ));
        then(loadFormSectionPort).should(never()).listByFormIds(any());
    }

    @Test
    @DisplayName("Form batch adapter가 owner scope 밖 child를 반환하면 fail closed한다")
    void form_batch의_scope_밖_child는_fail_closed한다() {
        // given
        given(loadFormPort.batchGetByIds(List.of(FORM_ID)))
            .willReturn(List.of(form(FORM_ID)));
        given(loadFormSectionPort.listByFormIds(List.of(FORM_ID)))
            .willReturn(List.of(FormSection.create(
                form(FOREIGN_FORM_ID), "foreign", null, 1L
            )));

        // when / then
        assertOwnershipDenied(() -> formQueryService.batchGetFormsWithStructure(
            List.of(owner(FORM_ID)), actor(MEMBER_ID)
        ));
        then(loadQuestionPort).should(never()).listBySectionIdIn(any());
    }

    @Test
    @DisplayName("Question ID 구조 조회가 요청하지 않은 question을 반환하면 거부한다")
    void question_ID_구조_조회의_요청하지_않은_question은_fail_closed한다() {
        // given
        given(loadFormPort.findById(FORM_ID)).willReturn(Optional.of(form(FORM_ID)));
        given(loadQuestionPort.listByIdIn(Set.of(10L)))
            .willReturn(List.of(question(11L, FORM_ID)));

        // when / then
        assertOwnershipDenied(() -> formQueryService.getFormWithStructureByQuestionIds(
            owner(FORM_ID), actor(MEMBER_ID), Set.of(10L)
        ));
        then(loadQuestionOptionPort).should(never()).listByQuestionIdIn(any());
    }

    @Test
    @DisplayName("Response batch의 expected owner scope 밖 response는 fail closed한다")
    void response_batch의_scope_밖_response는_fail_closed한다() {
        // given
        given(loadFormResponsePort.listByIdsWithForm(Set.of(1L)))
            .willReturn(List.of(namedResponse(1L, FOREIGN_FORM_ID)));

        // when / then
        assertOwnershipDenied(() -> formResponseQueryService.findResponsesWithAnswers(
            List.of(owner(FORM_ID)), actor(MEMBER_ID), Set.of(1L)
        ));
        then(getAnswerUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Response batch adapter가 요청하지 않은 같은 Form response를 반환하면 fail closed한다")
    void response_batch의_요청하지_않은_response는_fail_closed한다() {
        // given
        given(loadFormResponsePort.listByIdsWithForm(Set.of(1L)))
            .willReturn(List.of(namedResponse(2L, FORM_ID)));

        // when / then
        assertOwnershipDenied(() -> formResponseQueryService.findResponsesWithAnswers(
            List.of(owner(FORM_ID)), actor(MEMBER_ID), Set.of(1L)
        ));
        then(getAnswerUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Answer batch의 expected owner scope 밖 child는 fail closed한다")
    void answer_batch의_scope_밖_child는_fail_closed한다() {
        // given
        FormResponse scoped = namedResponse(1L, FORM_ID);
        FormResponse foreign = namedResponse(2L, FOREIGN_FORM_ID);
        given(loadFormResponsePort.listByIdsWithForm(Set.of(1L, 2L)))
            .willReturn(List.of(scoped));
        given(loadAnswerPort.listByFormResponseIds(Set.of(1L, 2L)))
            .willReturn(List.of(answer(10L, foreign)));

        // when / then
        assertOwnershipDenied(() -> answerQueryService.listByFormResponseIds(
            List.of(owner(FORM_ID)), actor(MEMBER_ID), Set.of(1L, 2L)
        ));
        then(loadAnswerPort).should(never()).listChoicesByAnswerIdIn(any());
    }

    @Test
    @DisplayName("Answer batch의 요청하지 않은 response child는 fail closed한다")
    void answer_batch의_요청하지_않은_response_child는_fail_closed한다() {
        // given
        FormResponse requested = namedResponse(1L, FORM_ID);
        FormResponse unrequested = namedResponse(2L, FORM_ID);
        given(loadFormResponsePort.listByIdsWithForm(Set.of(1L)))
            .willReturn(List.of(requested));
        given(loadAnswerPort.listByFormResponseIds(Set.of(1L)))
            .willReturn(List.of(answer(10L, unrequested)));

        // when / then
        assertOwnershipDenied(() -> answerQueryService.listByFormResponseIds(
            List.of(owner(FORM_ID)), actor(MEMBER_ID), Set.of(1L)
        ));
        then(loadAnswerPort).should(never()).listChoicesByAnswerIdIn(any());
    }

    private static Form form(Long formId) {
        Form form = Form.createDraft("폼", MEMBER_ID);
        ReflectionTestUtils.setField(form, "id", formId);
        return form;
    }

    private static FormResponse namedResponse(Long responseId, Long formId) {
        FormResponse response = FormResponse.createDraft(form(formId), MEMBER_ID);
        ReflectionTestUtils.setField(response, "id", responseId);
        return response;
    }

    private static Answer answer(Long answerId, FormResponse response) {
        Question question = Question.create("질문", QuestionType.SHORT_TEXT, false, 1L);
        ReflectionTestUtils.setField(question, "id", 10L);
        Answer answer = Answer.create(response, question, QuestionType.SHORT_TEXT, "답", null);
        ReflectionTestUtils.setField(answer, "id", answerId);
        return answer;
    }

    private static Question question(Long questionId, Long formId) {
        FormSection section = FormSection.create(form(formId), "섹션", null, 1L);
        ReflectionTestUtils.setField(section, "id", 20L);
        Question question = Question.create("질문", QuestionType.SHORT_TEXT, false, 1L);
        question.assignTo(section);
        ReflectionTestUtils.setField(question, "id", questionId);
        return question;
    }

    private static void assertOwnershipDenied(
        org.assertj.core.api.ThrowableAssert.ThrowingCallable callable
    ) {
        assertThatThrownBy(callable)
            .isInstanceOf(FormDomainException.class)
            .hasFieldOrPropertyWithValue("baseCode", FormErrorCode.FORM_OWNERSHIP_FORBIDDEN);
    }
}
