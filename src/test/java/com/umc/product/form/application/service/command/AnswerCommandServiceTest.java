package com.umc.product.form.application.service.command;

import static com.umc.product.form.application.service.FormAccessTestFixtures.actor;
import static com.umc.product.form.application.service.FormAccessTestFixtures.anonymous;
import static com.umc.product.form.application.service.FormAccessTestFixtures.owner;
import static com.umc.product.form.application.service.FormAccessTestFixtures.responseActor;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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
import com.umc.product.form.application.service.FormOwnershipAccessService;
import com.umc.product.form.domain.Answer;
import com.umc.product.form.domain.Form;
import com.umc.product.form.domain.FormOperation;
import com.umc.product.form.domain.FormResponse;
import com.umc.product.form.domain.FormSection;
import com.umc.product.form.domain.Question;
import com.umc.product.form.domain.enums.QuestionType;
import com.umc.product.form.domain.exception.FormDomainException;
import com.umc.product.form.domain.exception.FormErrorCode;
import com.umc.product.storage.application.port.in.query.GetFileUseCase;

@ExtendWith(MockitoExtension.class)
class AnswerCommandServiceTest {

    private static final Long FORM_ID = 100L;
    private static final Long FORM_RESPONSE_ID = 200L;
    private static final Long OWNER_MEMBER_ID = 300L;
    private static final Long OTHER_MEMBER_ID = 301L;
    private static final Long QUESTION_ID = 400L;
    private static final Long ANSWER_ID = 500L;

    @Mock
    LoadFormResponsePort loadFormResponsePort;
    @Mock
    LoadQuestionPort loadQuestionPort;
    @Mock
    LoadQuestionOptionPort loadQuestionOptionPort;
    @Mock
    LoadAnswerPort loadAnswerPort;
    @Mock
    SaveAnswerPort saveAnswerPort;
    @Mock
    SaveFormResponsePort saveFormResponsePort;
    @Mock
    GetFileUseCase getFileUseCase;
    @Mock
    SecureTokenGenerator secureTokenGenerator;
    @Mock
    FormOwnershipAccessService ownershipAccessService;

    @InjectMocks
    AnswerCommandService sut;

    private static final String RAW_KEY = "raw-key";
    private static final String KEY_HASH = "hash-value";
    private static final String OTHER_HASH = "other-hash";

    // ============================================================
    //          createAnswer — 기명 owner 검증
    // ============================================================

    @Test
    @DisplayName("createAnswer: requesterMemberId=null 이면 FORM_RESPONSE_FORBIDDEN")
    void createAnswer_requesterMemberId_null_FORBIDDEN() {
        FormResponse draft = namedDraft(OWNER_MEMBER_ID);
        given(loadFormResponsePort.findById(FORM_RESPONSE_ID)).willReturn(Optional.of(draft));

        assertThatThrownBy(() -> sut.createAnswer(owner(FORM_ID), anonymous(), CreateAnswerCommand.builder()
            .formResponseId(FORM_RESPONSE_ID)
            .questionId(QUESTION_ID)
            .textValue("답")
            .build()))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.FORM_RESPONSE_FORBIDDEN);

        then(saveAnswerPort).should(never()).save(any());
    }

    @Test
    @DisplayName("createAnswer: 소유자 불일치면 FORM_RESPONSE_FORBIDDEN")
    void createAnswer_소유자_불일치_FORBIDDEN() {
        FormResponse draft = namedDraft(OWNER_MEMBER_ID);
        given(loadFormResponsePort.findById(FORM_RESPONSE_ID)).willReturn(Optional.of(draft));

        assertThatThrownBy(() -> sut.createAnswer(owner(FORM_ID), actor(OTHER_MEMBER_ID), CreateAnswerCommand.builder()
            .formResponseId(FORM_RESPONSE_ID)
            .questionId(QUESTION_ID)
            .textValue("답")
            .build()))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.FORM_RESPONSE_FORBIDDEN);

        then(saveAnswerPort).should(never()).save(any());
    }

    @Test
    @DisplayName("createAnswer: 익명 draft 면 FORM_RESPONSE_FORBIDDEN")
    void createAnswer_익명_draft_FORBIDDEN() {
        FormResponse anonymousDraft = anonymousDraft();
        given(loadFormResponsePort.findById(FORM_RESPONSE_ID)).willReturn(Optional.of(anonymousDraft));

        assertThatThrownBy(() -> sut.createAnswer(owner(FORM_ID), actor(OWNER_MEMBER_ID), CreateAnswerCommand.builder()
            .formResponseId(FORM_RESPONSE_ID)
            .questionId(QUESTION_ID)
            .textValue("답")
            .build()))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.FORM_RESPONSE_FORBIDDEN);

        then(saveAnswerPort).should(never()).save(any());
    }

    // ============================================================
    //          updateAnswer — 기명 owner 검증
    // ============================================================

    @Test
    @DisplayName("updateAnswer: 소유자 불일치면 FORM_RESPONSE_FORBIDDEN")
    void updateAnswer_소유자_불일치_FORBIDDEN() {
        FormResponse draft = namedDraft(OWNER_MEMBER_ID);
        Answer answer = shortTextAnswer(draft);
        given(loadAnswerPort.findById(ANSWER_ID)).willReturn(Optional.of(answer));

        assertThatThrownBy(() -> sut.updateAnswer(owner(FORM_ID), actor(OTHER_MEMBER_ID), UpdateAnswerCommand.builder()
            .answerId(ANSWER_ID)
            .textValue("변경")
            .build()))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.FORM_RESPONSE_FORBIDDEN);

        then(saveAnswerPort).should(never()).save(any());
    }

    @Test
    @DisplayName("updateAnswer: 익명 draft 답변이면 FORM_RESPONSE_FORBIDDEN")
    void updateAnswer_익명_draft_답변_FORBIDDEN() {
        FormResponse anonymousDraft = anonymousDraft();
        Answer answer = shortTextAnswer(anonymousDraft);
        given(loadAnswerPort.findById(ANSWER_ID)).willReturn(Optional.of(answer));

        assertThatThrownBy(() -> sut.updateAnswer(owner(FORM_ID), actor(OWNER_MEMBER_ID), UpdateAnswerCommand.builder()
            .answerId(ANSWER_ID)
            .textValue("변경")
            .build()))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.FORM_RESPONSE_FORBIDDEN);

        then(saveAnswerPort).should(never()).save(any());
    }

    @Test
    @DisplayName("updateAnswer: 답변 없으면 ANSWER_NOT_FOUND")
    void updateAnswer_답변_없으면_NOT_FOUND() {
        given(loadAnswerPort.findById(ANSWER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> sut.updateAnswer(owner(FORM_ID), actor(OWNER_MEMBER_ID), UpdateAnswerCommand.builder()
            .answerId(ANSWER_ID)
            .textValue("변경")
            .build()))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.ANSWER_NOT_FOUND);
    }

    @Test
    @DisplayName("foreign answer 수정은 response-form parent chain ownership으로 거부한다")
    void foreign_answer_수정은_parent_form_ownership으로_거부한다() {
        Answer answer = shortTextAnswer(namedDraft(OWNER_MEMBER_ID));
        given(loadAnswerPort.findById(ANSWER_ID)).willReturn(Optional.of(answer));
        willThrow(new FormDomainException(FormErrorCode.FORM_OWNERSHIP_FORBIDDEN))
            .given(ownershipAccessService)
            .requireMutation(
                FORM_ID, owner(999L), actor(OWNER_MEMBER_ID), FormOperation.RESPOND
            );

        assertThatThrownBy(() -> sut.updateAnswer(
            owner(999L),
            actor(OWNER_MEMBER_ID),
            UpdateAnswerCommand.builder().answerId(ANSWER_ID).textValue("변경").build()
        ))
            .isInstanceOf(FormDomainException.class)
            .hasFieldOrPropertyWithValue("baseCode", FormErrorCode.FORM_OWNERSHIP_FORBIDDEN);

        then(saveAnswerPort).should(never()).save(any());
    }

    // ============================================================
    //          deleteAnswer — 기명 owner 검증
    // ============================================================

    @Test
    @DisplayName("deleteAnswer: 소유자 불일치면 FORM_RESPONSE_FORBIDDEN")
    void deleteAnswer_소유자_불일치_FORBIDDEN() {
        FormResponse draft = namedDraft(OWNER_MEMBER_ID);
        Answer answer = shortTextAnswer(draft);
        given(loadAnswerPort.findById(ANSWER_ID)).willReturn(Optional.of(answer));

        assertThatThrownBy(() -> sut.deleteAnswer(owner(FORM_ID), actor(OTHER_MEMBER_ID), DeleteAnswerCommand.builder()
            .answerId(ANSWER_ID)
            .build()))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.FORM_RESPONSE_FORBIDDEN);

        then(saveAnswerPort).should(never()).deleteByAnswerId(any());
    }

    @Test
    @DisplayName("deleteAnswer: 익명 draft 답변이면 FORM_RESPONSE_FORBIDDEN")
    void deleteAnswer_익명_draft_답변_FORBIDDEN() {
        FormResponse anonymousDraft = anonymousDraft();
        Answer answer = shortTextAnswer(anonymousDraft);
        given(loadAnswerPort.findById(ANSWER_ID)).willReturn(Optional.of(answer));

        assertThatThrownBy(() -> sut.deleteAnswer(owner(FORM_ID), actor(OWNER_MEMBER_ID), DeleteAnswerCommand.builder()
            .answerId(ANSWER_ID)
            .build()))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.FORM_RESPONSE_FORBIDDEN);

        then(saveAnswerPort).should(never()).deleteByAnswerId(any());
    }

    // ============================================================
    //          happy path — createAnswer 소유자 일치
    // ============================================================

    @Test
    @DisplayName("createAnswer: 소유자 일치면 답변 저장한다")
    void createAnswer_소유자_일치_저장() {
        FormResponse draft = namedDraft(OWNER_MEMBER_ID);
        Form form = draft.getForm();
        FormSection section = FormSection.create(form, "섹션", null, 1L);
        Question question = Question.create("질문", QuestionType.SHORT_TEXT, false, 1L);
        question.assignTo(section);
        ReflectionTestUtils.setField(question, "id", QUESTION_ID);

        given(loadFormResponsePort.findById(FORM_RESPONSE_ID)).willReturn(Optional.of(draft));
        given(loadQuestionPort.findById(QUESTION_ID)).willReturn(Optional.of(question));
        given(loadAnswerPort.existsByFormResponseIdAndQuestionId(FORM_RESPONSE_ID, QUESTION_ID))
            .willReturn(false);
        given(saveAnswerPort.save(any(Answer.class))).willAnswer(inv -> {
            Answer saved = inv.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", ANSWER_ID);
            return saved;
        });

        Long result = sut.createAnswer(owner(FORM_ID), actor(OWNER_MEMBER_ID), CreateAnswerCommand.builder()
            .formResponseId(FORM_RESPONSE_ID)
            .questionId(QUESTION_ID)
            .textValue("답")
            .build());

        org.assertj.core.api.Assertions.assertThat(result).isEqualTo(ANSWER_ID);
    }

    // ============================================================
    //          createAnonymousAnswer — 익명 access key 검증
    // ============================================================

    @Test
    @DisplayName("createAnonymousAnswer: rawKey null 이면 RESPONSE_ACCESS_KEY_REQUIRED")
    void createAnonymousAnswer_rawKey_null_예외() {
        assertThatThrownBy(() -> sut.createAnonymousAnswer(
            owner(FORM_ID), anonymous(), CreateAnonymousAnswerCommand.builder()
            .questionId(QUESTION_ID)
            .textValue("답")
            .build()))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.RESPONSE_ACCESS_KEY_REQUIRED);

        then(loadFormResponsePort).should(never()).findDraftByAccessKeyHash(any());
    }

    @Test
    @DisplayName("createAnonymousAnswer: hash 매칭 실패면 FORM_RESPONSE_FORBIDDEN")
    void createAnonymousAnswer_hash_매칭_실패_FORBIDDEN() {
        given(secureTokenGenerator.sha256Hex(RAW_KEY)).willReturn(KEY_HASH);
        given(loadFormResponsePort.findDraftByAccessKeyHash(KEY_HASH)).willReturn(Optional.empty());

        assertThatThrownBy(() -> sut.createAnonymousAnswer(
            owner(FORM_ID), responseActor(RAW_KEY), CreateAnonymousAnswerCommand.builder()
            .questionId(QUESTION_ID)
            .textValue("답")
            .build()))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.FORM_RESPONSE_FORBIDDEN);

        then(saveAnswerPort).should(never()).save(any());
    }

    @Test
    @DisplayName("createAnonymousAnswer: 매칭된 draft 가 기명이면 FORM_RESPONSE_FORBIDDEN")
    void createAnonymousAnswer_기명_draft_FORBIDDEN() {
        FormResponse namedDraft = namedDraft(OWNER_MEMBER_ID);
        given(secureTokenGenerator.sha256Hex(RAW_KEY)).willReturn(KEY_HASH);
        given(loadFormResponsePort.findDraftByAccessKeyHash(KEY_HASH)).willReturn(Optional.of(namedDraft));

        assertThatThrownBy(() -> sut.createAnonymousAnswer(
            owner(FORM_ID), responseActor(RAW_KEY), CreateAnonymousAnswerCommand.builder()
            .questionId(QUESTION_ID)
            .textValue("답")
            .build()))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.FORM_RESPONSE_FORBIDDEN);

        then(saveAnswerPort).should(never()).save(any());
    }

    @Test
    @DisplayName("createAnonymousAnswer: 익명 draft 면 답변 저장한다")
    void createAnonymousAnswer_익명_draft_저장() {
        FormResponse anonymousDraft = anonymousDraft();
        Form form = anonymousDraft.getForm();
        FormSection section = FormSection.create(form, "섹션", null, 1L);
        Question question = Question.create("질문", QuestionType.SHORT_TEXT, false, 1L);
        question.assignTo(section);
        ReflectionTestUtils.setField(question, "id", QUESTION_ID);

        given(secureTokenGenerator.sha256Hex(RAW_KEY)).willReturn(KEY_HASH);
        given(loadFormResponsePort.findDraftByAccessKeyHash(KEY_HASH)).willReturn(Optional.of(anonymousDraft));
        given(loadQuestionPort.findById(QUESTION_ID)).willReturn(Optional.of(question));
        given(loadAnswerPort.existsByFormResponseIdAndQuestionId(FORM_RESPONSE_ID, QUESTION_ID))
            .willReturn(false);
        given(saveAnswerPort.save(any(Answer.class))).willAnswer(inv -> {
            Answer saved = inv.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", ANSWER_ID);
            return saved;
        });

        Long result = sut.createAnonymousAnswer(
            owner(FORM_ID), responseActor(RAW_KEY), CreateAnonymousAnswerCommand.builder()
            .questionId(QUESTION_ID)
            .textValue("답")
            .build());

        org.assertj.core.api.Assertions.assertThat(result).isEqualTo(ANSWER_ID);
    }

    // ============================================================
    //          updateAnonymousAnswer — 익명 access key 검증
    // ============================================================

    @Test
    @DisplayName("updateAnonymousAnswer: rawKey null 이면 RESPONSE_ACCESS_KEY_REQUIRED")
    void updateAnonymousAnswer_rawKey_null_예외() {
        assertThatThrownBy(() -> sut.updateAnonymousAnswer(
            owner(FORM_ID), anonymous(), UpdateAnonymousAnswerCommand.builder()
            .answerId(ANSWER_ID)
            .textValue("변경")
            .build()))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.RESPONSE_ACCESS_KEY_REQUIRED);

        then(loadAnswerPort).should(never()).findById(any());
    }

    @Test
    @DisplayName("updateAnonymousAnswer: 답변의 draft 가 기명이면 FORM_RESPONSE_FORBIDDEN")
    void updateAnonymousAnswer_기명_draft_답변_FORBIDDEN() {
        FormResponse namedDraft = namedDraft(OWNER_MEMBER_ID);
        Answer answer = shortTextAnswer(namedDraft);
        given(loadAnswerPort.findById(ANSWER_ID)).willReturn(Optional.of(answer));

        assertThatThrownBy(() -> sut.updateAnonymousAnswer(
            owner(FORM_ID), responseActor(RAW_KEY), UpdateAnonymousAnswerCommand.builder()
            .answerId(ANSWER_ID)
            .textValue("변경")
            .build()))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.FORM_RESPONSE_FORBIDDEN);

        then(saveAnswerPort).should(never()).save(any());
    }

    @Test
    @DisplayName("updateAnonymousAnswer: Answer 없으면 FORM_RESPONSE_FORBIDDEN (익명 경계 유출 방지)")
    void updateAnonymousAnswer_Answer_없으면_FORBIDDEN() {
        given(loadAnswerPort.findById(ANSWER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> sut.updateAnonymousAnswer(
            owner(FORM_ID), responseActor(RAW_KEY), UpdateAnonymousAnswerCommand.builder()
            .answerId(ANSWER_ID)
            .textValue("변경")
            .build()))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.FORM_RESPONSE_FORBIDDEN);
    }

    @Test
    @DisplayName("updateAnonymousAnswer: hash 불일치면 FORM_RESPONSE_FORBIDDEN")
    void updateAnonymousAnswer_hash_불일치_FORBIDDEN() {
        FormResponse anonymousDraft = anonymousDraft();
        Answer answer = shortTextAnswer(anonymousDraft);
        given(loadAnswerPort.findById(ANSWER_ID)).willReturn(Optional.of(answer));
        given(secureTokenGenerator.sha256Hex(RAW_KEY)).willReturn(OTHER_HASH);

        assertThatThrownBy(() -> sut.updateAnonymousAnswer(
            owner(FORM_ID), responseActor(RAW_KEY), UpdateAnonymousAnswerCommand.builder()
            .answerId(ANSWER_ID)
            .textValue("변경")
            .build()))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.FORM_RESPONSE_FORBIDDEN);

        then(saveAnswerPort).should(never()).save(any());
    }

    // ============================================================
    //          deleteAnonymousAnswer — 익명 access key 검증
    // ============================================================

    @Test
    @DisplayName("deleteAnonymousAnswer: rawKey null 이면 RESPONSE_ACCESS_KEY_REQUIRED")
    void deleteAnonymousAnswer_rawKey_null_예외() {
        assertThatThrownBy(() -> sut.deleteAnonymousAnswer(
            owner(FORM_ID), anonymous(), DeleteAnonymousAnswerCommand.builder()
            .answerId(ANSWER_ID)
            .build()))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.RESPONSE_ACCESS_KEY_REQUIRED);

        then(loadAnswerPort).should(never()).findById(any());
    }

    @Test
    @DisplayName("deleteAnonymousAnswer: hash 불일치면 FORM_RESPONSE_FORBIDDEN")
    void deleteAnonymousAnswer_hash_불일치_FORBIDDEN() {
        FormResponse anonymousDraft = anonymousDraft();
        Answer answer = shortTextAnswer(anonymousDraft);
        given(loadAnswerPort.findById(ANSWER_ID)).willReturn(Optional.of(answer));
        given(secureTokenGenerator.sha256Hex(RAW_KEY)).willReturn(OTHER_HASH);

        assertThatThrownBy(() -> sut.deleteAnonymousAnswer(
            owner(FORM_ID), responseActor(RAW_KEY), DeleteAnonymousAnswerCommand.builder()
            .answerId(ANSWER_ID)
            .build()))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.FORM_RESPONSE_FORBIDDEN);

        then(saveAnswerPort).should(never()).deleteByAnswerId(any());
    }

    @Test
    @DisplayName("deleteAnonymousAnswer: hash 일치하는 익명 draft 답변이면 삭제한다")
    void deleteAnonymousAnswer_hash_일치_삭제() {
        FormResponse anonymousDraft = anonymousDraft();
        Answer answer = shortTextAnswer(anonymousDraft);
        given(loadAnswerPort.findById(ANSWER_ID)).willReturn(Optional.of(answer));
        given(secureTokenGenerator.sha256Hex(RAW_KEY)).willReturn(KEY_HASH);

        sut.deleteAnonymousAnswer(owner(FORM_ID), responseActor(RAW_KEY), DeleteAnonymousAnswerCommand.builder()
            .answerId(ANSWER_ID)
            .build());

        then(saveAnswerPort).should().deleteByAnswerId(ANSWER_ID);
    }

    private FormResponse namedDraft(Long memberId) {
        Form form = publishedForm();
        FormResponse draft = FormResponse.createDraft(form, memberId);
        ReflectionTestUtils.setField(draft, "id", FORM_RESPONSE_ID);
        return draft;
    }

    private FormResponse anonymousDraft() {
        Form form = publishedForm();
        FormResponse draft = FormResponse.createAnonymousDraft(form, "hash-value");
        ReflectionTestUtils.setField(draft, "id", FORM_RESPONSE_ID);
        return draft;
    }

    private Form publishedForm() {
        Form form = Form.createDraft("폼", 1L, false);
        ReflectionTestUtils.setField(form, "id", FORM_ID);
        form.publish();
        return form;
    }

    private Answer shortTextAnswer(FormResponse formResponse) {
        Question question = Question.create("질문", QuestionType.SHORT_TEXT, false, 1L);
        ReflectionTestUtils.setField(question, "id", QUESTION_ID);
        Answer answer = Answer.create(formResponse, question, QuestionType.SHORT_TEXT, "답", null);
        ReflectionTestUtils.setField(answer, "id", ANSWER_ID);
        return answer;
    }
}
