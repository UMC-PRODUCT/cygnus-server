package com.umc.product.form.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Instant;
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
import com.umc.product.form.application.port.in.command.dto.AnswerCommand;
import com.umc.product.form.application.port.in.command.dto.CreateDraftFormResponseCommand;
import com.umc.product.form.application.port.in.command.dto.DeleteAnonymousDraftFormResponseCommand;
import com.umc.product.form.application.port.in.command.dto.DeleteAnonymousFormResponseCommand;
import com.umc.product.form.application.port.in.command.dto.DeleteDraftFormResponseCommand;
import com.umc.product.form.application.port.in.command.dto.DeleteFormResponseCommand;
import com.umc.product.form.application.port.in.command.dto.SubmitAnonymousDraftFormResponseCommand;
import com.umc.product.form.application.port.in.command.dto.SubmitFormResponseCommand;
import com.umc.product.form.application.port.in.command.dto.UpdateAnonymousDraftFormResponseCommand;
import com.umc.product.form.application.port.in.command.dto.UpdateAnonymousFormResponseCommand;
import com.umc.product.form.application.port.in.command.dto.UpdateDraftFormResponseCommand;
import com.umc.product.form.application.port.in.command.dto.UpdateFormResponseCommand;
import com.umc.product.form.application.port.out.LoadAnswerPort;
import com.umc.product.form.application.port.out.LoadFormPort;
import com.umc.product.form.application.port.out.LoadFormResponsePort;
import com.umc.product.form.application.port.out.LoadFormSectionPort;
import com.umc.product.form.application.port.out.LoadQuestionOptionPort;
import com.umc.product.form.application.port.out.LoadQuestionPort;
import com.umc.product.form.application.port.out.SaveAnswerPort;
import com.umc.product.form.application.port.out.SaveFormResponsePort;
import com.umc.product.form.domain.Answer;
import com.umc.product.form.domain.AnswerChoice;
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
@DisplayName("FormResponseCommandService 잔여 lifecycle·형식 경계")
class FormResponseCommandServiceResidualTest {

    private static final Long FORM_ID = 1L;
    private static final Long RESPONSE_ID = 2L;
    private static final Long MEMBER_ID = 3L;
    private static final Long QUESTION_ID = 4L;
    private static final String RAW_KEY = "raw-key";
    private static final String HASH = "hash";

    @Mock LoadFormPort loadFormPort;
    @Mock LoadFormSectionPort loadFormSectionPort;
    @Mock LoadQuestionPort loadQuestionPort;
    @Mock LoadQuestionOptionPort loadQuestionOptionPort;
    @Mock LoadFormResponsePort loadFormResponsePort;
    @Mock LoadAnswerPort loadAnswerPort;
    @Mock SaveFormResponsePort saveFormResponsePort;
    @Mock SaveAnswerPort saveAnswerPort;
    @Mock GetFileUseCase getFileUseCase;
    @Mock SecureTokenGenerator secureTokenGenerator;

    FormResponseCommandService sut;

    @BeforeEach
    void setUp() {
        sut = new FormResponseCommandService(
            loadFormPort, loadFormSectionPort, loadQuestionPort, loadQuestionOptionPort,
            loadFormResponsePort, loadAnswerPort, saveFormResponsePort, saveAnswerPort,
            getFileUseCase, secureTokenGenerator
        );
        org.mockito.Mockito.lenient().doAnswer(invocation -> invocation.getArgument(0))
            .when(saveAnswerPort).saveAll(anyList());
    }

    @Test
    @DisplayName("기명 제출 응답 수정은 기존 답변을 교체하고 allowed 미답변을 snapshot으로 저장한다")
    void updates_named_submitted_response_and_saves_empty_answers() {
        Form form = publishedForm(false);
        FormResponse response = namedSubmitted(form);
        FormSection section = section(form, 10L, 1L);
        Question answered = question(section, QUESTION_ID, QuestionType.SHORT_TEXT, true, 1L);
        Question unanswered = question(section, 5L, QuestionType.LONG_TEXT, false, 2L);
        given(loadFormPort.findById(FORM_ID)).willReturn(Optional.of(form));
        given(loadFormResponsePort.findSubmittedByFormIdAndRespondentMemberId(FORM_ID, MEMBER_ID))
            .willReturn(Optional.of(response));
        given(loadQuestionPort.listByFormId(FORM_ID)).willReturn(List.of(answered, unanswered));
        given(loadFormSectionPort.listByFormId(FORM_ID)).willReturn(List.of(section));
        given(loadQuestionPort.listByIdIn(Set.of(5L))).willReturn(List.of(unanswered));

        sut.updateResponse(UpdateFormResponseCommand.builder()
            .formId(FORM_ID).respondentMemberId(MEMBER_ID)
            .answers(List.of(textAnswer(QUESTION_ID, "답")))
            .allowedQuestionIds(Set.of(QUESTION_ID, 5L))
            .requiredQuestionIds(Set.of(QUESTION_ID))
            .build());

        then(saveAnswerPort).should().deleteAllByFormResponseId(RESPONSE_ID);
        then(saveAnswerPort).should(org.mockito.Mockito.times(2)).saveAll(any());
        then(saveFormResponsePort).should().save(response);
    }

    @Test
    @DisplayName("기명 제출 응답 삭제는 answer를 먼저 지우고 response를 삭제한다")
    void deletes_named_submitted_response() {
        Form form = publishedForm(false);
        FormResponse response = namedSubmitted(form);
        given(loadFormPort.findById(FORM_ID)).willReturn(Optional.of(form));
        given(loadFormResponsePort.findSubmittedByFormIdAndRespondentMemberId(FORM_ID, MEMBER_ID))
            .willReturn(Optional.of(response));

        sut.deleteResponse(DeleteFormResponseCommand.builder()
            .formId(FORM_ID).respondentMemberId(MEMBER_ID).build());

        then(saveAnswerPort).should().deleteAllByFormResponseId(RESPONSE_ID);
        then(saveFormResponsePort).should().deleteById(RESPONSE_ID);
    }

    @Test
    @DisplayName("기명 draft 수정은 객관식 option snapshot을 포함해 전체 답변을 교체한다")
    void updates_named_draft_with_choices() {
        Form form = publishedForm(false);
        FormResponse draft = namedDraft(form);
        FormSection section = section(form, 10L, 1L);
        Question question = question(section, QUESTION_ID, QuestionType.RADIO, false, 1L);
        QuestionOption option = option(question, 20L, "선택", 1L, null);
        given(loadFormResponsePort.findById(RESPONSE_ID)).willReturn(Optional.of(draft));
        given(loadQuestionPort.listByFormId(FORM_ID)).willReturn(List.of(question));
        given(loadQuestionOptionPort.existsByIdAndQuestionId(20L, QUESTION_ID)).willReturn(true);
        given(loadQuestionOptionPort.listByQuestionId(QUESTION_ID)).willReturn(List.of(option));

        sut.updateDraft(UpdateDraftFormResponseCommand.builder()
            .formResponseId(RESPONSE_ID).requesterMemberId(MEMBER_ID)
            .answers(List.of(optionAnswer(QUESTION_ID, 20L))).build());

        then(saveAnswerPort).should().deleteAllByFormResponseId(RESPONSE_ID);
        then(saveAnswerPort).should().saveAllChoices(org.mockito.ArgumentMatchers.argThat(
            choices -> choices.size() == 1 && choices.get(0).getAnsweredAsContent().equals("선택")));
        then(saveFormResponsePort).should().save(draft);
    }

    @Test
    @DisplayName("기명 draft 삭제는 answer와 response를 함께 제거한다")
    void deletes_named_draft() {
        FormResponse draft = namedDraft(publishedForm(false));
        given(loadFormResponsePort.findById(RESPONSE_ID)).willReturn(Optional.of(draft));

        sut.deleteDraft(DeleteDraftFormResponseCommand.builder()
            .formResponseId(RESPONSE_ID).requesterMemberId(MEMBER_ID).build());

        then(saveAnswerPort).should().deleteAllByFormResponseId(RESPONSE_ID);
        then(saveFormResponsePort).should().deleteById(RESPONSE_ID);
    }

    @Test
    @DisplayName("익명 제출 응답 수정은 파일을 검증·중복 제거해 답변을 교체한다")
    void updates_anonymous_submitted_response_with_files() {
        Form form = publishedForm(false);
        FormResponse response = anonymousSubmitted(form);
        FormSection section = section(form, 10L, 1L);
        Question question = question(section, QUESTION_ID, QuestionType.FILE, true, 1L);
        stubSubmittedAnonymous(response);
        given(loadQuestionPort.listByFormId(FORM_ID)).willReturn(List.of(question));
        given(loadFormSectionPort.listByFormId(FORM_ID)).willReturn(List.of(section));

        sut.updateAnonymousResponse(UpdateAnonymousFormResponseCommand.builder()
            .responseAccessKey(RAW_KEY)
            .answers(List.of(fileAnswer(QUESTION_ID, List.of("file", "file"))))
            .allowedQuestionIds(Set.of(QUESTION_ID))
            .requiredQuestionIds(Set.of(QUESTION_ID))
            .build());

        then(getFileUseCase).should(org.mockito.Mockito.times(2)).throwIfNotExists("file");
        then(saveAnswerPort).should().saveAll(org.mockito.ArgumentMatchers.argThat(answers ->
            answers.size() == 1 && answers.get(0).getFileIds().equals(Set.of("file"))));
        then(saveFormResponsePort).should().save(response);
    }

    @Test
    @DisplayName("익명 제출 응답 삭제는 access key 검증 뒤 answer와 response를 제거한다")
    void deletes_anonymous_submitted_response() {
        FormResponse response = anonymousSubmitted(publishedForm(false));
        stubSubmittedAnonymous(response);

        sut.deleteAnonymousResponse(DeleteAnonymousFormResponseCommand.builder()
            .responseAccessKey(RAW_KEY).build());

        then(saveAnswerPort).should().deleteAllByFormResponseId(RESPONSE_ID);
        then(saveFormResponsePort).should().deleteById(RESPONSE_ID);
    }

    @Test
    @DisplayName("익명 draft 수정은 CHECKBOX 선택값을 검증하고 snapshot choices로 교체한다")
    void updates_anonymous_draft_with_checkbox_choices() {
        Form form = publishedForm(false);
        FormResponse draft = anonymousDraft(form);
        FormSection section = section(form, 10L, 1L);
        Question question = question(section, QUESTION_ID, QuestionType.CHECKBOX, false, 1L);
        QuestionOption first = option(question, 20L, "A", 1L, null);
        QuestionOption second = option(question, 21L, "B", 2L, null);
        stubDraftAnonymous(draft);
        given(loadQuestionPort.listByFormId(FORM_ID)).willReturn(List.of(question));
        given(loadQuestionOptionPort.existsByIdAndQuestionId(20L, QUESTION_ID)).willReturn(true);
        given(loadQuestionOptionPort.existsByIdAndQuestionId(21L, QUESTION_ID)).willReturn(true);
        given(loadQuestionOptionPort.listByQuestionId(QUESTION_ID)).willReturn(List.of(first, second));

        sut.updateAnonymousDraft(UpdateAnonymousDraftFormResponseCommand.builder()
            .responseAccessKey(RAW_KEY)
            .answers(List.of(AnswerCommand.builder().questionId(QUESTION_ID)
                .selectedOptionIds(List.of(20L, 21L)).build()))
            .build());

        then(saveAnswerPort).should().saveAllChoices(org.mockito.ArgumentMatchers.argThat(
            choices -> choices.size() == 2));
        then(saveFormResponsePort).should().save(draft);
    }

    @Test
    @DisplayName("익명 draft 제출은 여러 choice 중 첫 값을 경로 계산에 사용하고 SUBMITTED로 전이한다")
    void submits_anonymous_draft_with_multiple_choices() {
        Form form = publishedForm(false);
        FormResponse draft = anonymousDraft(form);
        FormSection section = section(form, 10L, 1L);
        Question question = question(section, QUESTION_ID, QuestionType.CHECKBOX, true, 1L);
        QuestionOption first = option(question, 20L, "A", 1L, null);
        QuestionOption second = option(question, 21L, "B", 2L, null);
        Answer answer = answer(draft, question, 30L);
        AnswerChoice firstChoice = AnswerChoice.create(answer, first);
        AnswerChoice secondChoice = AnswerChoice.create(answer, second);
        AnswerChoice deletedChoice = AnswerChoice.create(answer, second);
        ReflectionTestUtils.setField(deletedChoice, "questionOption", null);
        stubDraftAnonymous(draft);
        given(loadAnswerPort.listByFormResponseId(RESPONSE_ID)).willReturn(List.of(answer));
        given(loadAnswerPort.listChoicesByAnswerIdIn(Set.of(30L)))
            .willReturn(List.of(firstChoice, secondChoice, deletedChoice));
        given(loadFormSectionPort.listByFormId(FORM_ID)).willReturn(List.of(section));
        given(loadQuestionPort.listByFormId(FORM_ID)).willReturn(List.of(question));

        sut.submitAnonymousDraft(SubmitAnonymousDraftFormResponseCommand.builder()
            .responseAccessKey(RAW_KEY).submittedIp("127.0.0.1")
            .allowedQuestionIds(Set.of(QUESTION_ID)).requiredQuestionIds(Set.of(QUESTION_ID))
            .build());

        assertThat(draft.getStatus()).isEqualTo(com.umc.product.form.domain.enums.FormResponseStatus.SUBMITTED);
        assertThat(draft.getSubmittedIp()).isEqualTo("127.0.0.1");
        then(saveFormResponsePort).should().save(draft);
    }

    @Test
    @DisplayName("익명 draft 삭제는 access key 검증 뒤 answer와 response를 제거한다")
    void deletes_anonymous_draft() {
        FormResponse draft = anonymousDraft(publishedForm(false));
        stubDraftAnonymous(draft);

        sut.deleteAnonymousDraft(DeleteAnonymousDraftFormResponseCommand.builder()
            .responseAccessKey(RAW_KEY).build());

        then(saveAnswerPort).should().deleteAllByFormResponseId(RESPONSE_ID);
        then(saveFormResponsePort).should().deleteById(RESPONSE_ID);
    }

    @Test
    @DisplayName("일반 기명 draft 생성은 중복이 없을 때 새 응답 ID를 반환한다")
    void creates_named_draft_when_duplicate_does_not_exist() {
        Form form = publishedForm(false);
        given(loadFormPort.findById(FORM_ID)).willReturn(Optional.of(form));
        given(saveFormResponsePort.save(any())).willAnswer(invocation -> {
            FormResponse value = invocation.getArgument(0);
            ReflectionTestUtils.setField(value, "id", RESPONSE_ID);
            return value;
        });

        Long result = sut.createDraft(CreateDraftFormResponseCommand.builder()
            .formId(FORM_ID).respondentMemberId(MEMBER_ID).build());

        assertThat(result).isEqualTo(RESPONSE_ID);
        then(loadFormResponsePort).should().existsByFormIdAndMemberId(FORM_ID, MEMBER_ID);
    }

    @Test
    @DisplayName("없는 form·제출 응답·draft와 이미 제출된 draft 접근을 구분한다")
    void distinguishes_missing_and_non_draft_boundaries() {
        given(loadFormPort.findById(404L)).willReturn(Optional.empty());
        assertError(() -> sut.updateResponse(UpdateFormResponseCommand.builder()
            .formId(404L).respondentMemberId(MEMBER_ID).answers(List.of()).build()),
            FormErrorCode.FORM_NOT_FOUND);

        Form form = publishedForm(false);
        given(loadFormPort.findById(FORM_ID)).willReturn(Optional.of(form));
        given(loadFormResponsePort.findSubmittedByFormIdAndRespondentMemberId(FORM_ID, MEMBER_ID))
            .willReturn(Optional.empty());
        assertError(() -> sut.updateResponse(UpdateFormResponseCommand.builder()
            .formId(FORM_ID).respondentMemberId(MEMBER_ID).answers(List.of()).build()),
            FormErrorCode.FORM_RESPONSE_NOT_FOUND);
        assertError(() -> sut.deleteResponse(DeleteFormResponseCommand.builder()
            .formId(FORM_ID).respondentMemberId(MEMBER_ID).build()),
            FormErrorCode.FORM_RESPONSE_NOT_FOUND);

        given(loadFormResponsePort.findById(404L)).willReturn(Optional.empty());
        assertError(() -> sut.updateDraft(UpdateDraftFormResponseCommand.builder()
            .formResponseId(404L).requesterMemberId(MEMBER_ID).answers(List.of()).build()),
            FormErrorCode.FORM_RESPONSE_NOT_FOUND);

        FormResponse submitted = namedSubmitted(form);
        given(loadFormResponsePort.findById(RESPONSE_ID)).willReturn(Optional.of(submitted));
        assertError(() -> sut.updateDraft(UpdateDraftFormResponseCommand.builder()
            .formResponseId(RESPONSE_ID).requesterMemberId(MEMBER_ID).answers(List.of()).build()),
            FormErrorCode.FORM_RESPONSE_NOT_DRAFT);
    }

    @Test
    @DisplayName("answer 목록은 null question·중복 question·다른 form 질문을 거부한다")
    void rejects_invalid_answer_collections() {
        FormResponse draft = namedDraft(publishedForm(false));
        given(loadFormResponsePort.findById(RESPONSE_ID)).willReturn(Optional.of(draft));
        assertUpdateDraftError(null, FormErrorCode.INVALID_ANSWER_FORMAT);

        given(loadQuestionPort.listByFormId(FORM_ID)).willReturn(List.of());
        assertUpdateDraftError(List.of(textAnswer(null, "답")), FormErrorCode.INVALID_ANSWER_FORMAT);

        FormSection section = section(draft.getForm(), 10L, 1L);
        Question question = question(section, QUESTION_ID, QuestionType.SHORT_TEXT, false, 1L);
        given(loadQuestionPort.listByFormId(FORM_ID)).willReturn(List.of(question));
        assertUpdateDraftError(
            List.of(textAnswer(QUESTION_ID, "첫 답"), textAnswer(QUESTION_ID, "두 답")),
            FormErrorCode.INVALID_ANSWER_FORMAT);
        assertUpdateDraftError(List.of(textAnswer(999L, "답")),
            FormErrorCode.QUESTION_IS_NOT_OWNED_BY_FORM);
    }

    @Test
    @DisplayName("객관식·파일 답변은 개수와 질문 소속을 fail-closed로 검증한다")
    void validates_objective_and_file_formats() {
        FormResponse draft = namedDraft(publishedForm(false));
        given(loadFormResponsePort.findById(RESPONSE_ID)).willReturn(Optional.of(draft));
        FormSection section = section(draft.getForm(), 10L, 1L);

        Question radio = question(section, QUESTION_ID, QuestionType.RADIO, false, 1L);
        given(loadQuestionPort.listByFormId(FORM_ID)).willReturn(List.of(radio));
        assertUpdateDraftError(List.of(AnswerCommand.builder().questionId(QUESTION_ID).build()),
            FormErrorCode.INVALID_VOTE_SELECTION);
        assertUpdateDraftError(List.of(AnswerCommand.builder().questionId(QUESTION_ID)
            .selectedOptionIds(List.of(20L, 21L)).build()), FormErrorCode.INVALID_VOTE_SELECTION);
        assertUpdateDraftError(List.of(optionAnswer(QUESTION_ID, 20L)),
            FormErrorCode.OPTION_NOT_IN_QUESTION);

        Question checkbox = question(section, QUESTION_ID, QuestionType.CHECKBOX, false, 1L);
        given(loadQuestionPort.listByFormId(FORM_ID)).willReturn(List.of(checkbox));
        assertUpdateDraftError(List.of(AnswerCommand.builder().questionId(QUESTION_ID)
            .selectedOptionIds(List.of()).build()), FormErrorCode.INVALID_VOTE_SELECTION);
        assertUpdateDraftError(List.of(optionAnswer(QUESTION_ID, 20L)),
            FormErrorCode.OPTION_NOT_IN_QUESTION);

        Question file = question(section, QUESTION_ID, QuestionType.FILE, false, 1L);
        given(loadQuestionPort.listByFormId(FORM_ID)).willReturn(List.of(file));
        assertUpdateDraftError(List.of(fileAnswer(QUESTION_ID, List.of())),
            FormErrorCode.INVALID_ANSWER_FORMAT);
    }

    @Test
    @DisplayName("PORTFOLIO는 text 또는 검증된 file을 허용하고 둘 다 없으면 거부한다")
    void validates_portfolio_alternatives() {
        FormResponse draft = namedDraft(publishedForm(false));
        given(loadFormResponsePort.findById(RESPONSE_ID)).willReturn(Optional.of(draft));
        FormSection section = section(draft.getForm(), 10L, 1L);
        Question portfolio = question(section, QUESTION_ID, QuestionType.PORTFOLIO, false, 1L);
        given(loadQuestionPort.listByFormId(FORM_ID)).willReturn(List.of(portfolio));

        assertUpdateDraftError(List.of(AnswerCommand.builder().questionId(QUESTION_ID)
            .textValue(" ").fileIds(List.of()).build()), FormErrorCode.INVALID_ANSWER_FORMAT);

        sut.updateDraft(updateDraft(List.of(textAnswer(QUESTION_ID, "https://portfolio"))));
        sut.updateDraft(updateDraft(List.of(fileAnswer(QUESTION_ID, List.of("portfolio-file")))));

        then(getFileUseCase).should().throwIfNotExists("portfolio-file");
    }

    @Test
    @DisplayName("SCHEDULE은 지원 전임을 명시적인 예외로 알린다")
    void schedule_is_explicitly_unsupported() {
        FormResponse draft = namedDraft(publishedForm(false));
        FormSection section = section(draft.getForm(), 10L, 1L);
        Question schedule = question(section, QUESTION_ID, QuestionType.SCHEDULE, false, 1L);
        given(loadFormResponsePort.findById(RESPONSE_ID)).willReturn(Optional.of(draft));
        given(loadQuestionPort.listByFormId(FORM_ID)).willReturn(List.of(schedule));

        assertThatThrownBy(() -> sut.updateDraft(updateDraft(List.of(
            AnswerCommand.builder().questionId(QUESTION_ID).build()))))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("SCHEDULE");
    }

    @Test
    @DisplayName("validation 이후 질문·option snapshot이 사라진 저장 race도 fail-closed로 처리한다")
    void build_phase_revalidates_question_and_option_snapshots() {
        FormResponse draft = namedDraft(publishedForm(false));
        FormSection section = section(draft.getForm(), 10L, 1L);
        Question text = question(section, QUESTION_ID, QuestionType.SHORT_TEXT, false, 1L);
        given(loadFormResponsePort.findById(RESPONSE_ID)).willReturn(Optional.of(draft));
        given(loadQuestionPort.listByFormId(FORM_ID))
            .willReturn(List.of(text), List.of());
        assertUpdateDraftError(List.of(textAnswer(QUESTION_ID, "답")), FormErrorCode.QUESTION_NOT_FOUND);

        Question radio = question(section, QUESTION_ID, QuestionType.RADIO, false, 1L);
        given(loadQuestionPort.listByFormId(FORM_ID)).willReturn(List.of(radio));
        given(loadQuestionOptionPort.existsByIdAndQuestionId(20L, QUESTION_ID)).willReturn(true);
        given(loadQuestionOptionPort.listByQuestionId(QUESTION_ID)).willReturn(List.of());
        assertUpdateDraftError(List.of(optionAnswer(QUESTION_ID, 20L)),
            FormErrorCode.OPTION_NOT_IN_QUESTION);
    }

    @Test
    @DisplayName("조건부 경로는 선택 jump와 선택 없는 순차 이동을 모두 계산한다")
    void resolves_jump_and_sequential_paths() {
        Form jumpForm = publishedForm(true);
        FormSection first = section(jumpForm, 10L, 1L);
        FormSection skipped = section(jumpForm, 11L, 2L);
        FormSection destination = section(jumpForm, 12L, 3L);
        Question radio = question(first, QUESTION_ID, QuestionType.RADIO, true, 1L);
        QuestionOption jump = option(radio, 20L, "건너뛰기", 1L, destination.getId());
        given(loadFormPort.findById(FORM_ID)).willReturn(Optional.of(jumpForm));
        given(loadQuestionPort.listByFormId(FORM_ID)).willReturn(List.of(radio));
        given(loadFormSectionPort.listByFormId(FORM_ID)).willReturn(List.of(destination, first, skipped));
        given(loadQuestionOptionPort.existsByIdAndQuestionId(20L, QUESTION_ID)).willReturn(true);
        given(loadQuestionOptionPort.listByQuestionIdIn(Set.of(QUESTION_ID))).willReturn(List.of(jump));
        given(loadQuestionOptionPort.listByQuestionId(QUESTION_ID)).willReturn(List.of(jump));
        given(saveFormResponsePort.save(any())).willAnswer(invocation -> {
            FormResponse value = invocation.getArgument(0);
            ReflectionTestUtils.setField(value, "id", RESPONSE_ID);
            return value;
        });

        assertThat(sut.submitImmediately(SubmitFormResponseCommand.builder()
            .formId(FORM_ID).respondentMemberId(MEMBER_ID)
            .answers(List.of(optionAnswer(QUESTION_ID, 20L))).build())).isEqualTo(RESPONSE_ID);

        Form sequentialForm = publishedForm(true);
        FormSection sequentialFirst = section(sequentialForm, 30L, 1L);
        FormSection sequentialSecond = section(sequentialForm, 31L, 2L);
        Question optionalRadio = question(sequentialFirst, 40L, QuestionType.RADIO, false, 1L);
        QuestionOption noJump = option(optionalRadio, 41L, "다음", 1L, null);
        given(loadFormPort.findById(FORM_ID)).willReturn(Optional.of(sequentialForm));
        given(loadQuestionPort.listByFormId(FORM_ID)).willReturn(List.of(optionalRadio));
        given(loadFormSectionPort.listByFormId(FORM_ID))
            .willReturn(List.of(sequentialSecond, sequentialFirst));
        given(loadQuestionOptionPort.existsByIdAndQuestionId(41L, 40L)).willReturn(true);
        given(loadQuestionOptionPort.listByQuestionIdIn(Set.of(40L))).willReturn(List.of(noJump));
        given(loadQuestionOptionPort.listByQuestionId(40L)).willReturn(List.of(noJump));

        sut.submitImmediately(SubmitFormResponseCommand.builder()
            .formId(FORM_ID).respondentMemberId(MEMBER_ID)
            .answers(List.of(optionAnswer(40L, 41L))).build());
    }

    @Test
    @DisplayName("저장된 답변이 없는 익명 draft도 optional 구조에서는 제출할 수 있다")
    void submits_empty_anonymous_draft_when_nothing_is_required() {
        Form form = publishedForm(false);
        FormResponse draft = anonymousDraft(form);
        FormSection section = section(form, 10L, 1L);
        Question optional = question(section, QUESTION_ID, QuestionType.SHORT_TEXT, false, 1L);
        stubDraftAnonymous(draft);
        given(loadAnswerPort.listByFormResponseId(RESPONSE_ID)).willReturn(List.of());
        given(loadFormSectionPort.listByFormId(FORM_ID)).willReturn(List.of(section));
        given(loadQuestionPort.listByFormId(FORM_ID)).willReturn(List.of(optional));

        sut.submitAnonymousDraft(SubmitAnonymousDraftFormResponseCommand.builder()
            .responseAccessKey(RAW_KEY).build());

        assertThat(draft.getStatus()).isEqualTo(com.umc.product.form.domain.enums.FormResponseStatus.SUBMITTED);
    }

    private void stubSubmittedAnonymous(FormResponse response) {
        given(secureTokenGenerator.sha256Hex(RAW_KEY)).willReturn(HASH);
        given(loadFormResponsePort.findSubmittedByAccessKeyHash(HASH)).willReturn(Optional.of(response));
    }

    private void stubDraftAnonymous(FormResponse response) {
        given(secureTokenGenerator.sha256Hex(RAW_KEY)).willReturn(HASH);
        given(loadFormResponsePort.findDraftByAccessKeyHash(HASH)).willReturn(Optional.of(response));
    }

    private Form publishedForm(boolean allowDuplicate) {
        Form form = Form.createPublished(1L, "폼", false, allowDuplicate);
        ReflectionTestUtils.setField(form, "id", FORM_ID);
        return form;
    }

    private FormResponse namedDraft(Form form) {
        FormResponse response = FormResponse.createDraft(form, MEMBER_ID);
        ReflectionTestUtils.setField(response, "id", RESPONSE_ID);
        return response;
    }

    private FormResponse namedSubmitted(Form form) {
        FormResponse response = namedDraft(form);
        response.submit(Instant.parse("2026-01-01T00:00:00Z"), null);
        return response;
    }

    private FormResponse anonymousDraft(Form form) {
        FormResponse response = FormResponse.createAnonymousDraft(form, HASH);
        ReflectionTestUtils.setField(response, "id", RESPONSE_ID);
        return response;
    }

    private FormResponse anonymousSubmitted(Form form) {
        FormResponse response = anonymousDraft(form);
        response.submit(Instant.parse("2026-01-01T00:00:00Z"), null);
        return response;
    }

    private FormSection section(Form form, Long id, long orderNo) {
        FormSection section = FormSection.create(form, "섹션", null, orderNo);
        ReflectionTestUtils.setField(section, "id", id);
        return section;
    }

    private Question question(
        FormSection section,
        Long id,
        QuestionType type,
        boolean required,
        long orderNo
    ) {
        Question question = Question.create("질문", type, required, orderNo);
        question.assignTo(section);
        ReflectionTestUtils.setField(question, "id", id);
        return question;
    }

    private QuestionOption option(
        Question question,
        Long id,
        String content,
        long orderNo,
        Long nextSectionId
    ) {
        QuestionOption option = QuestionOption.create(content, orderNo, false, nextSectionId);
        option.assignTo(question);
        ReflectionTestUtils.setField(option, "id", id);
        return option;
    }

    private Answer answer(FormResponse response, Question question, Long id) {
        Answer answer = Answer.create(response, question, question.getType(), null, null);
        ReflectionTestUtils.setField(answer, "id", id);
        return answer;
    }

    private AnswerCommand textAnswer(Long questionId, String text) {
        return AnswerCommand.builder().questionId(questionId).textValue(text).build();
    }

    private AnswerCommand optionAnswer(Long questionId, Long optionId) {
        return AnswerCommand.builder().questionId(questionId)
            .selectedOptionIds(List.of(optionId)).build();
    }

    private AnswerCommand fileAnswer(Long questionId, List<String> fileIds) {
        return AnswerCommand.builder().questionId(questionId).fileIds(fileIds).build();
    }

    private UpdateDraftFormResponseCommand updateDraft(List<AnswerCommand> answers) {
        return UpdateDraftFormResponseCommand.builder()
            .formResponseId(RESPONSE_ID).requesterMemberId(MEMBER_ID).answers(answers).build();
    }

    private void assertUpdateDraftError(List<AnswerCommand> answers, FormErrorCode code) {
        assertError(() -> sut.updateDraft(updateDraft(answers)), code);
    }

    private void assertError(Runnable action, FormErrorCode code) {
        assertThatThrownBy(action::run)
            .isInstanceOfSatisfying(FormDomainException.class, exception ->
                assertThat(exception.getBaseCode()).isEqualTo(code));
    }
}
