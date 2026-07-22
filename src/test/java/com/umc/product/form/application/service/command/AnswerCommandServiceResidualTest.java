package com.umc.product.form.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

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
import com.umc.product.form.application.port.in.command.dto.CreateAnonymousAnswerCommand;
import com.umc.product.form.application.port.in.command.dto.CreateAnswerCommand;
import com.umc.product.form.application.port.in.command.dto.DeleteAnonymousAnswerCommand;
import com.umc.product.form.application.port.in.command.dto.DeleteAnswerCommand;
import com.umc.product.form.application.port.in.command.dto.UpdateAnonymousAnswerCommand;
import com.umc.product.form.application.port.in.command.dto.UpdateAnswerCommand;
import com.umc.product.form.application.port.out.LoadAnswerPort;
import com.umc.product.form.application.port.out.LoadFormResponsePort;
import com.umc.product.form.application.port.out.LoadQuestionOptionPort;
import com.umc.product.form.application.port.out.LoadQuestionPort;
import com.umc.product.form.application.port.out.SaveAnswerPort;
import com.umc.product.form.application.port.out.SaveFormResponsePort;
import com.umc.product.form.domain.Answer;
import com.umc.product.form.domain.Form;
import com.umc.product.form.domain.FormResponse;
import com.umc.product.form.domain.FormSection;
import com.umc.product.form.domain.Question;
import com.umc.product.form.domain.QuestionOption;
import com.umc.product.form.domain.enums.QuestionType;
import com.umc.product.form.domain.exception.FormDomainException;
import com.umc.product.form.domain.exception.FormErrorCode;
import com.umc.product.storage.application.port.in.query.GetFileUseCase;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnswerCommandService 타입·상태 잔여 경계")
class AnswerCommandServiceResidualTest {

    private static final Long FORM_ID = 1L;
    private static final Long RESPONSE_ID = 2L;
    private static final Long QUESTION_ID = 3L;
    private static final Long ANSWER_ID = 4L;
    private static final Long MEMBER_ID = 5L;
    private static final String RAW_KEY = "raw";
    private static final String HASH = "hash";

    @Mock LoadFormResponsePort loadFormResponsePort;
    @Mock LoadQuestionPort loadQuestionPort;
    @Mock LoadQuestionOptionPort loadQuestionOptionPort;
    @Mock LoadAnswerPort loadAnswerPort;
    @Mock SaveAnswerPort saveAnswerPort;
    @Mock SaveFormResponsePort saveFormResponsePort;
    @Mock GetFileUseCase getFileUseCase;
    @Mock SecureTokenGenerator secureTokenGenerator;

    AnswerCommandService sut;

    @BeforeEach
    void setUp() {
        sut = new AnswerCommandService(
            loadFormResponsePort, loadQuestionPort, loadQuestionOptionPort, loadAnswerPort,
            saveAnswerPort, saveFormResponsePort, getFileUseCase, secureTokenGenerator
        );
    }

    @Test
    @DisplayName("기명 답변 수정은 기존 choice를 지우고 Answer PK를 유지해 저장한다")
    void updates_named_answer_and_keeps_null_files() {
        FormResponse draft = namedDraft();
        Answer answer = answer(draft, question(QuestionType.SHORT_TEXT), "기존", Set.of("기존-file"));
        given(loadAnswerPort.findById(ANSWER_ID)).willReturn(Optional.of(answer));

        sut.updateAnswer(UpdateAnswerCommand.builder()
            .answerId(ANSWER_ID).requesterMemberId(MEMBER_ID).textValue("변경").fileIds(null).build());

        assertThat(answer.getTextValue()).isEqualTo("변경");
        assertThat(answer.getFileIds()).containsExactly("기존-file");
        then(saveAnswerPort).should().deleteChoicesByAnswerId(ANSWER_ID);
        then(saveAnswerPort).should().save(answer);
        then(saveFormResponsePort).should().save(draft);
    }

    @Test
    @DisplayName("기명 객관식 수정은 새 파일 집합과 choice를 함께 교체한다")
    void updates_named_choice_and_replaces_files() {
        FormResponse draft = namedDraft();
        Question question = question(QuestionType.RADIO);
        Answer answer = answer(draft, question, null, Set.of("old"));
        QuestionOption option = option(10L, question, "선택");
        given(loadAnswerPort.findById(ANSWER_ID)).willReturn(Optional.of(answer));
        given(loadQuestionOptionPort.existsByIdAndQuestionId(10L, QUESTION_ID)).willReturn(true);
        given(loadQuestionOptionPort.listByQuestionId(QUESTION_ID)).willReturn(List.of(option));

        sut.updateAnswer(UpdateAnswerCommand.builder()
            .answerId(ANSWER_ID).requesterMemberId(MEMBER_ID)
            .selectedOptionIds(List.of(10L)).fileIds(List.of("new", "new")).build());

        assertThat(answer.getFileIds()).containsExactly("new");
        then(saveAnswerPort).should().saveAllChoices(org.mockito.ArgumentMatchers.argThat(
            choices -> choices.size() == 1 && choices.get(0).getQuestionOption() == option));
    }

    @Test
    @DisplayName("기명 답변 삭제는 answer를 제거하고 draft 저장 시각을 갱신한다")
    void deletes_named_answer() {
        FormResponse draft = namedDraft();
        Answer answer = answer(draft, question(QuestionType.SHORT_TEXT), "답", null);
        var before = draft.getLastSavedAt();
        given(loadAnswerPort.findById(ANSWER_ID)).willReturn(Optional.of(answer));

        sut.deleteAnswer(DeleteAnswerCommand.builder()
            .answerId(ANSWER_ID).requesterMemberId(MEMBER_ID).build());

        then(saveAnswerPort).should().deleteByAnswerId(ANSWER_ID);
        then(saveFormResponsePort).should().save(draft);
        assertThat(draft.getLastSavedAt()).isAfterOrEqualTo(before);
    }

    @Test
    @DisplayName("익명 CHECKBOX 수정은 hash를 검증하고 파일을 비우며 choices를 교체한다")
    void updates_anonymous_choices_and_clears_files() {
        FormResponse draft = anonymousDraft();
        Question question = question(QuestionType.CHECKBOX);
        Answer answer = answer(draft, question, null, Set.of("old"));
        QuestionOption first = option(10L, question, "A");
        QuestionOption second = option(11L, question, "B");
        given(loadAnswerPort.findById(ANSWER_ID)).willReturn(Optional.of(answer));
        given(secureTokenGenerator.sha256Hex(RAW_KEY)).willReturn(HASH);
        given(loadQuestionOptionPort.existsByIdAndQuestionId(10L, QUESTION_ID)).willReturn(true);
        given(loadQuestionOptionPort.existsByIdAndQuestionId(11L, QUESTION_ID)).willReturn(true);
        given(loadQuestionOptionPort.listByQuestionId(QUESTION_ID)).willReturn(List.of(first, second));

        sut.updateAnonymousAnswer(UpdateAnonymousAnswerCommand.builder()
            .answerId(ANSWER_ID).responseAccessKey(RAW_KEY)
            .selectedOptionIds(List.of(10L, 11L)).fileIds(null).build());

        assertThat(answer.getFileIds()).containsExactly("old");
        then(saveAnswerPort).should().deleteChoicesByAnswerId(ANSWER_ID);
        then(saveAnswerPort).should().saveAllChoices(org.mockito.ArgumentMatchers.argThat(
            choices -> choices.size() == 2));
        then(saveFormResponsePort).should().save(draft);
    }

    @Test
    @DisplayName("익명 답변 삭제 성공은 hash 일치 후 answer ID를 제거한다")
    void deletes_anonymous_answer() {
        FormResponse draft = anonymousDraft();
        Answer answer = answer(draft, question(QuestionType.SHORT_TEXT), "답", null);
        given(loadAnswerPort.findById(ANSWER_ID)).willReturn(Optional.of(answer));
        given(secureTokenGenerator.sha256Hex(RAW_KEY)).willReturn(HASH);

        sut.deleteAnonymousAnswer(DeleteAnonymousAnswerCommand.builder()
            .answerId(ANSWER_ID).responseAccessKey(RAW_KEY).build());

        then(saveAnswerPort).should().deleteByAnswerId(ANSWER_ID);
        then(saveFormResponsePort).should().save(draft);
    }

    @Test
    @DisplayName("익명 객관식 생성은 access key와 option 소속을 확인한 뒤 choice를 저장한다")
    void creates_anonymous_choice() {
        FormResponse draft = anonymousDraft();
        Question question = question(QuestionType.DROPDOWN);
        QuestionOption option = option(10L, question, "선택");
        given(secureTokenGenerator.sha256Hex(RAW_KEY)).willReturn(HASH);
        given(loadFormResponsePort.findDraftByAccessKeyHash(HASH)).willReturn(Optional.of(draft));
        given(loadQuestionPort.findById(QUESTION_ID)).willReturn(Optional.of(question));
        given(loadQuestionOptionPort.existsByIdAndQuestionId(10L, QUESTION_ID)).willReturn(true);
        given(loadQuestionOptionPort.listByQuestionId(QUESTION_ID)).willReturn(List.of(option));
        given(saveAnswerPort.save(any())).willAnswer(invocation -> {
            Answer value = invocation.getArgument(0);
            ReflectionTestUtils.setField(value, "id", ANSWER_ID);
            return value;
        });

        Long answerId = sut.createAnonymousAnswer(CreateAnonymousAnswerCommand.builder()
            .responseAccessKey(RAW_KEY).questionId(QUESTION_ID)
            .selectedOptionIds(List.of(10L)).build());

        assertThat(answerId).isEqualTo(ANSWER_ID);
        then(saveAnswerPort).should().saveAllChoices(org.mockito.ArgumentMatchers.argThat(
            choices -> choices.size() == 1 && choices.get(0).getQuestionOption() == option));
    }

    @Test
    @DisplayName("없는·제출 완료 answer와 없는·제출 완료 response는 수정 경계에서 구분한다")
    void missing_and_non_draft_boundaries() {
        given(loadFormResponsePort.findById(404L)).willReturn(Optional.empty());
        assertError(() -> sut.createAnswer(namedCreate(404L, "답", null, null)),
            FormErrorCode.FORM_RESPONSE_NOT_FOUND);

        FormResponse submitted = namedDraft();
        submitted.submit(java.time.Instant.now(), "ip");
        given(loadFormResponsePort.findById(RESPONSE_ID)).willReturn(Optional.of(submitted));
        assertError(() -> sut.createAnswer(namedCreate(RESPONSE_ID, "답", null, null)),
            FormErrorCode.FORM_RESPONSE_NOT_DRAFT);

        Answer submittedAnswer = answer(submitted, question(QuestionType.SHORT_TEXT), "답", null);
        given(loadAnswerPort.findById(ANSWER_ID)).willReturn(Optional.of(submittedAnswer));
        assertError(() -> sut.updateAnswer(UpdateAnswerCommand.builder()
            .answerId(ANSWER_ID).requesterMemberId(MEMBER_ID).textValue("변경").build()),
            FormErrorCode.FORM_RESPONSE_NOT_DRAFT);

        FormResponse anonymousSubmitted = anonymousDraft();
        anonymousSubmitted.submit(java.time.Instant.now(), "ip");
        Answer anonymousAnswer = answer(
            anonymousSubmitted, question(QuestionType.SHORT_TEXT), "답", null);
        given(loadAnswerPort.findById(ANSWER_ID)).willReturn(Optional.of(anonymousAnswer));
        assertError(() -> sut.updateAnonymousAnswer(UpdateAnonymousAnswerCommand.builder()
            .answerId(ANSWER_ID).responseAccessKey(RAW_KEY).textValue("변경").build()),
            FormErrorCode.FORM_RESPONSE_FORBIDDEN);
    }

    @Test
    @DisplayName("질문 부재와 다른 폼 소속 질문은 각각 NOT_FOUND와 NOT_OWNED를 반환한다")
    void question_presence_and_ownership() {
        FormResponse draft = namedDraft();
        given(loadFormResponsePort.findById(RESPONSE_ID)).willReturn(Optional.of(draft));
        given(loadQuestionPort.findById(404L)).willReturn(Optional.empty());
        assertError(() -> sut.createAnswer(CreateAnswerCommand.builder()
            .formResponseId(RESPONSE_ID).questionId(404L).requesterMemberId(MEMBER_ID).textValue("답").build()),
            FormErrorCode.QUESTION_NOT_FOUND);

        Question other = question(QuestionType.SHORT_TEXT);
        ReflectionTestUtils.setField(other.getFormSection().getForm(), "id", 999L);
        given(loadQuestionPort.findById(QUESTION_ID)).willReturn(Optional.of(other));
        assertError(() -> sut.createAnswer(namedCreate(RESPONSE_ID, "답", null, null)),
            FormErrorCode.QUESTION_IS_NOT_OWNED_BY_FORM);
    }

    @Test
    @DisplayName("같은 질문의 기명·익명 중복 답변 생성을 모두 거부한다")
    void rejects_duplicate_named_and_anonymous_answers() {
        FormResponse named = namedDraft();
        Question question = question(QuestionType.SHORT_TEXT);
        given(loadFormResponsePort.findById(RESPONSE_ID)).willReturn(Optional.of(named));
        given(loadQuestionPort.findById(QUESTION_ID)).willReturn(Optional.of(question));
        given(loadAnswerPort.existsByFormResponseIdAndQuestionId(RESPONSE_ID, QUESTION_ID)).willReturn(true);
        assertError(() -> sut.createAnswer(namedCreate(RESPONSE_ID, "답", null, null)),
            FormErrorCode.ANSWER_ALREADY_EXISTS);

        FormResponse anonymous = anonymousDraft();
        given(secureTokenGenerator.sha256Hex(RAW_KEY)).willReturn(HASH);
        given(loadFormResponsePort.findDraftByAccessKeyHash(HASH)).willReturn(Optional.of(anonymous));
        assertError(() -> sut.createAnonymousAnswer(CreateAnonymousAnswerCommand.builder()
            .responseAccessKey(RAW_KEY).questionId(QUESTION_ID).textValue("답").build()),
            FormErrorCode.ANSWER_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("SHORT/LONG 텍스트는 non-blank만 허용하고 non-objective 선택값은 choice로 저장하지 않는다")
    void validates_text_types_and_ignores_irrelevant_choices() {
        assertNamedCreateError(QuestionType.SHORT_TEXT, null, null, null, FormErrorCode.INVALID_ANSWER_FORMAT);
        assertNamedCreateError(QuestionType.LONG_TEXT, " ", null, null, FormErrorCode.INVALID_ANSWER_FORMAT);

        Long id = createNamed(QuestionType.LONG_TEXT, "답", List.of(999L), null, List.of());
        assertThat(id).isEqualTo(ANSWER_ID);
        then(saveAnswerPort).should(org.mockito.Mockito.never()).saveAllChoices(any());
    }

    @Test
    @DisplayName("RADIO/DROPDOWN은 정확히 한 개이면서 소속 질문의 option만 허용한다")
    void validates_single_choice_types() {
        assertNamedCreateError(QuestionType.RADIO, null, null, null, FormErrorCode.INVALID_VOTE_SELECTION);
        assertNamedCreateError(QuestionType.DROPDOWN, null, List.of(10L, 11L), null,
            FormErrorCode.INVALID_VOTE_SELECTION);
        assertOptionMembershipRejected(QuestionType.RADIO);

        createNamed(QuestionType.DROPDOWN, null, List.of(10L), null, List.of(optionPlaceholder(10L)));
        then(saveAnswerPort).should().saveAllChoices(org.mockito.ArgumentMatchers.argThat(
            choices -> choices.size() == 1 && choices.get(0).getAnsweredAsContent().equals("보기10")));
    }

    @Test
    @DisplayName("CHECKBOX는 한 개 이상 소속 option만 허용하고 조회 결과 누락도 거부한다")
    void validates_multiple_choice_type_and_loaded_options() {
        assertNamedCreateError(QuestionType.CHECKBOX, null, List.of(), null,
            FormErrorCode.INVALID_VOTE_SELECTION);
        assertOptionMembershipRejected(QuestionType.CHECKBOX);

        assertThatThrownBy(() -> createNamed(
            QuestionType.CHECKBOX, null, List.of(10L), null, List.of()))
            .isInstanceOfSatisfying(FormDomainException.class, exception ->
                assertThat(exception.getBaseCode()).isEqualTo(FormErrorCode.OPTION_NOT_IN_QUESTION));
    }

    @Test
    @DisplayName("FILE은 파일을 필수로 검증·중복 제거해 저장하고 PORTFOLIO는 텍스트 또는 파일을 허용한다")
    void validates_file_and_portfolio_types() {
        assertNamedCreateError(QuestionType.FILE, null, null, List.of(), FormErrorCode.INVALID_ANSWER_FORMAT);
        createNamed(QuestionType.FILE, null, null, List.of("f1", "f1"), List.of());
        then(getFileUseCase).should(org.mockito.Mockito.times(2)).throwIfNotExists("f1");
        then(saveAnswerPort).should().save(org.mockito.ArgumentMatchers.argThat(
            value -> value.getFileIds().equals(Set.of("f1"))));

        assertNamedCreateError(QuestionType.PORTFOLIO, " ", null, List.of(),
            FormErrorCode.INVALID_ANSWER_FORMAT);
        createNamed(QuestionType.PORTFOLIO, "소개", null, List.of(), List.of());
        createNamed(QuestionType.PORTFOLIO, null, null, List.of("portfolio"), List.of());
        then(getFileUseCase).should().throwIfNotExists("portfolio");
    }

    @Test
    @DisplayName("SCHEDULE은 지원 전임을 명시적인 예외로 알린다")
    void schedule_is_explicitly_unsupported() {
        assertThatThrownBy(() -> createNamed(QuestionType.SCHEDULE, null, null, null, List.of()))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("SCHEDULE");
    }

    private void assertNamedCreateError(
        QuestionType type,
        String text,
        List<Long> selected,
        List<String> files,
        FormErrorCode code
    ) {
        assertError(() -> createNamed(type, text, selected, files, List.of()), code);
    }

    private void assertOptionMembershipRejected(QuestionType type) {
        FormResponse draft = namedDraft();
        Question question = question(type);
        given(loadFormResponsePort.findById(RESPONSE_ID)).willReturn(Optional.of(draft));
        given(loadQuestionPort.findById(QUESTION_ID)).willReturn(Optional.of(question));
        assertError(() -> sut.createAnswer(namedCreate(RESPONSE_ID, null, List.of(10L), null)),
            FormErrorCode.OPTION_NOT_IN_QUESTION);
    }

    private Long createNamed(
        QuestionType type,
        String text,
        List<Long> selected,
        List<String> files,
        List<QuestionOption> loadedOptions
    ) {
        FormResponse draft = namedDraft();
        Question question = question(type);
        given(loadFormResponsePort.findById(RESPONSE_ID)).willReturn(Optional.of(draft));
        given(loadQuestionPort.findById(QUESTION_ID)).willReturn(Optional.of(question));
        given(loadAnswerPort.existsByFormResponseIdAndQuestionId(RESPONSE_ID, QUESTION_ID)).willReturn(false);
        if (selected != null) {
            for (Long optionId : selected) {
                org.mockito.Mockito.lenient()
                    .when(loadQuestionOptionPort.existsByIdAndQuestionId(optionId, QUESTION_ID))
                    .thenReturn(true);
            }
        }
        org.mockito.Mockito.lenient().doAnswer(invocation -> {
            Answer value = invocation.getArgument(0);
            ReflectionTestUtils.setField(value, "id", ANSWER_ID);
            return value;
        }).when(saveAnswerPort).save(any());
        if (selected != null && !selected.isEmpty()
            && (type == QuestionType.RADIO || type == QuestionType.CHECKBOX || type == QuestionType.DROPDOWN)) {
            org.mockito.Mockito.lenient()
                .when(loadQuestionOptionPort.listByQuestionId(QUESTION_ID))
                .thenReturn(loadedOptions);
        }
        return sut.createAnswer(namedCreate(RESPONSE_ID, text, selected, files));
    }

    private CreateAnswerCommand namedCreate(
        Long responseId,
        String text,
        List<Long> selected,
        List<String> files
    ) {
        return CreateAnswerCommand.builder()
            .formResponseId(responseId).questionId(QUESTION_ID).requesterMemberId(MEMBER_ID)
            .textValue(text).selectedOptionIds(selected).fileIds(files).build();
    }

    private FormResponse namedDraft() {
        FormResponse response = FormResponse.createDraft(form(), MEMBER_ID);
        ReflectionTestUtils.setField(response, "id", RESPONSE_ID);
        return response;
    }

    private FormResponse anonymousDraft() {
        FormResponse response = FormResponse.createAnonymousDraft(form(), HASH);
        ReflectionTestUtils.setField(response, "id", RESPONSE_ID);
        return response;
    }

    private Form form() {
        Form form = Form.createPublished(1L, "폼", false);
        ReflectionTestUtils.setField(form, "id", FORM_ID);
        return form;
    }

    private Question question(QuestionType type) {
        FormSection section = FormSection.create(form(), "섹션", null, 1L);
        ReflectionTestUtils.setField(section, "id", 10L);
        Question question = Question.create("질문", type, true, 1L);
        question.assignTo(section);
        ReflectionTestUtils.setField(question, "id", QUESTION_ID);
        return question;
    }

    private Answer answer(FormResponse response, Question question, String text, Set<String> files) {
        Answer answer = Answer.create(response, question, question.getType(), text, files);
        ReflectionTestUtils.setField(answer, "id", ANSWER_ID);
        return answer;
    }

    private QuestionOption option(Long id, Question question, String content) {
        QuestionOption option = QuestionOption.create(content, id, false);
        option.assignTo(question);
        ReflectionTestUtils.setField(option, "id", id);
        return option;
    }

    private QuestionOption optionPlaceholder(Long id) {
        return option(id, question(QuestionType.RADIO), "보기" + id);
    }

    private void assertError(Runnable action, FormErrorCode code) {
        assertThatThrownBy(action::run)
            .isInstanceOfSatisfying(FormDomainException.class, exception ->
                assertThat(exception.getBaseCode()).isEqualTo(code));
    }
}
