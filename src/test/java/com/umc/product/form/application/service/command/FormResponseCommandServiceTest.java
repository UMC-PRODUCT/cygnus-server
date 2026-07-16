package com.umc.product.form.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.authentication.application.service.SecureTokenGenerator;
import com.umc.product.form.application.port.in.command.dto.AnonymousFormResponseResult;
import com.umc.product.form.application.port.in.command.dto.AnswerCommand;
import com.umc.product.form.application.port.in.command.dto.CreateAnonymousDraftFormResponseCommand;
import com.umc.product.form.application.port.in.command.dto.CreateDraftFormResponseCommand;
import com.umc.product.form.application.port.in.command.dto.DeleteAnonymousDraftFormResponseCommand;
import com.umc.product.form.application.port.in.command.dto.DeleteAnonymousFormResponseCommand;
import com.umc.product.form.application.port.in.command.dto.DeleteDraftFormResponseCommand;
import com.umc.product.form.application.port.in.command.dto.DeleteFormResponseCommand;
import com.umc.product.form.application.port.in.command.dto.SubmitAnonymousDraftFormResponseCommand;
import com.umc.product.form.application.port.in.command.dto.SubmitAnonymousImmediatelyFormResponseCommand;
import com.umc.product.form.application.port.in.command.dto.SubmitDraftFormResponseCommand;
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
class   FormResponseCommandServiceTest {

    private static final Long FORM_ID = 100L;
    private static final Long MEMBER_ID = 200L;
    private static final Long FORM_RESPONSE_ID = 300L;

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
    SaveFormResponsePort saveFormResponsePort;
    @Mock
    SaveAnswerPort saveAnswerPort;
    @Mock
    GetFileUseCase getFileUseCase;
    @Mock
    SecureTokenGenerator secureTokenGenerator;

    @InjectMocks
    FormResponseCommandService sut;

    @Test
    @DisplayName("기본 폼은 같은 form/member의 draft 생성을 차단한다")
    void 기본_폼은_중복_draft_생성을_차단한다() {
        given(loadFormPort.findById(FORM_ID)).willReturn(Optional.of(publishedForm(false)));
        given(loadFormResponsePort.existsByFormIdAndMemberId(FORM_ID, MEMBER_ID)).willReturn(true);

        assertThatThrownBy(() -> sut.createDraft(CreateDraftFormResponseCommand.builder()
            .formId(FORM_ID)
            .respondentMemberId(MEMBER_ID)
            .build()))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.FORM_RESPONSE_ALREADY_EXISTS);

        then(saveFormResponsePort).should(never()).save(any());
    }

    @Test
    @DisplayName("기본 폼은 같은 form/member의 즉시 제출을 차단한다")
    void 기본_폼은_중복_즉시_제출을_차단한다() {
        given(loadFormPort.findById(FORM_ID)).willReturn(Optional.of(publishedForm(false)));
        given(loadFormResponsePort.existsByFormIdAndMemberId(FORM_ID, MEMBER_ID)).willReturn(true);

        assertThatThrownBy(() -> sut.submitImmediately(SubmitFormResponseCommand.builder()
            .formId(FORM_ID)
            .respondentMemberId(MEMBER_ID)
            .answers(List.of())
            .build()))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.FORM_RESPONSE_ALREADY_EXISTS);

        then(saveFormResponsePort).should(never()).save(any());
    }

    @Test
    @DisplayName("중복 허용 폼은 같은 form/member의 두 번째 draft도 새 응답으로 생성한다")
    void 중복_허용_폼은_두번째_draft도_생성한다() {
        given(loadFormPort.findById(FORM_ID)).willReturn(Optional.of(publishedForm(true)));
        given(saveFormResponsePort.save(any(FormResponse.class))).willAnswer(invocation -> {
            FormResponse response = invocation.getArgument(0);
            ReflectionTestUtils.setField(response, "id", FORM_RESPONSE_ID);
            return response;
        });

        Long result = sut.createDraft(CreateDraftFormResponseCommand.builder()
            .formId(FORM_ID)
            .respondentMemberId(MEMBER_ID)
            .build());

        assertThat(result).isEqualTo(FORM_RESPONSE_ID);
        then(loadFormResponsePort).should(never()).existsByFormIdAndMemberId(FORM_ID, MEMBER_ID);
    }

    @Test
    @DisplayName("중복 허용 폼은 같은 form/member의 즉시 제출도 새 응답으로 생성한다")
    void 중복_허용_폼은_두번째_즉시_제출도_생성한다() {
        given(loadFormPort.findById(FORM_ID)).willReturn(Optional.of(publishedForm(true)));
        given(loadQuestionPort.listByFormId(FORM_ID)).willReturn(List.of());
        given(saveFormResponsePort.save(any(FormResponse.class))).willAnswer(invocation -> {
            FormResponse response = invocation.getArgument(0);
            ReflectionTestUtils.setField(response, "id", FORM_RESPONSE_ID);
            return response;
        });

        Long result = sut.submitImmediately(SubmitFormResponseCommand.builder()
            .formId(FORM_ID)
            .respondentMemberId(MEMBER_ID)
            .answers(List.of())
            .build());

        assertThat(result).isEqualTo(FORM_RESPONSE_ID);
        then(loadFormResponsePort).should(never()).existsByFormIdAndMemberId(FORM_ID, MEMBER_ID);
    }

    @Test
    @DisplayName("중복 허용 폼은 formId/memberId 기반 제출 응답 수정을 막는다")
    void 중복_허용_폼은_단건_응답_수정을_막는다() {
        given(loadFormPort.findById(FORM_ID)).willReturn(Optional.of(publishedForm(true)));

        assertThatThrownBy(() -> sut.updateResponse(UpdateFormResponseCommand.builder()
            .formId(FORM_ID)
            .respondentMemberId(MEMBER_ID)
            .answers(List.of())
            .build()))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.FORM_RESPONSE_LOOKUP_AMBIGUOUS);

        then(loadFormResponsePort).should(never())
            .findSubmittedByFormIdAndRespondentMemberId(anyLong(), anyLong());
    }

    @Test
    @DisplayName("중복 허용 폼은 formId/memberId 기반 제출 응답 삭제를 막는다")
    void 중복_허용_폼은_단건_응답_삭제를_막는다() {
        given(loadFormPort.findById(FORM_ID)).willReturn(Optional.of(publishedForm(true)));

        assertThatThrownBy(() -> sut.deleteResponse(DeleteFormResponseCommand.builder()
            .formId(FORM_ID)
            .respondentMemberId(MEMBER_ID)
            .build()))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.FORM_RESPONSE_LOOKUP_AMBIGUOUS);

        then(loadFormResponsePort).should(never())
            .findSubmittedByFormIdAndRespondentMemberId(anyLong(), anyLong());
        then(saveFormResponsePort).should(never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("draft 제출 scope가 있으면 전달된 required question만 필수 응답 검증한다")
    void draft_제출_scope가_있으면_전달된_required_question만_검증한다() {
        FormResponse draft = draftResponse();
        FormSection section = section(1L, 1L);
        Question commonRequiredQuestion = questionInSection(10L, section, true);
        Answer answered = answer(draft, commonRequiredQuestion);
        ReflectionTestUtils.setField(answered, "id", 1000L);
        given(loadFormResponsePort.findById(FORM_RESPONSE_ID)).willReturn(Optional.of(draft));
        given(loadAnswerPort.listByFormResponseId(FORM_RESPONSE_ID)).willReturn(List.of(answered));
        given(loadFormSectionPort.listByFormId(FORM_ID)).willReturn(List.of(section));
        given(loadQuestionPort.listByFormId(FORM_ID)).willReturn(List.of(commonRequiredQuestion));

        sut.submitDraft(SubmitDraftFormResponseCommand.builder()
            .formResponseId(FORM_RESPONSE_ID)
            .requesterMemberId(MEMBER_ID)
            .requiredQuestionIds(Set.of(commonRequiredQuestion.getId()))
            .allowedQuestionIds(Set.of(commonRequiredQuestion.getId()))
            .build());

        then(saveFormResponsePort).should().save(draft);
    }

    @Test
    @DisplayName("마감된 Form의 기존 draft는 제출할 수 없다")
    void 마감된_Form의_draft_제출을_거부한다() {
        FormResponse draft = draftResponse();
        draft.getForm().close();
        given(loadFormResponsePort.findById(FORM_RESPONSE_ID)).willReturn(Optional.of(draft));

        assertThatThrownBy(() -> sut.submitDraft(SubmitDraftFormResponseCommand.builder()
            .formResponseId(FORM_RESPONSE_ID)
            .requesterMemberId(MEMBER_ID)
            .build()))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.FORM_NOT_PUBLISHED);

        then(saveFormResponsePort).should(never()).save(any());
    }

    @Test
    @DisplayName("draft 제출 scope가 없으면 방문한 섹션의 required question을 검증한다")
    void draft_제출_scope가_없으면_방문한_섹션의_required_question을_검증한다() {
        FormResponse draft = draftResponse();
        FormSection section = section(1L, 1L);
        Question answeredRequiredQuestion = questionInSection(10L, section, true);
        Question missingRequiredQuestion = questionInSection(20L, section, true);

        given(loadFormResponsePort.findById(FORM_RESPONSE_ID)).willReturn(Optional.of(draft));
        given(loadAnswerPort.listByFormResponseId(FORM_RESPONSE_ID))
            .willReturn(List.of(answer(draft, answeredRequiredQuestion)));
        given(loadFormSectionPort.listByFormId(FORM_ID)).willReturn(List.of(section));
        given(loadQuestionPort.listByFormId(FORM_ID))
            .willReturn(List.of(answeredRequiredQuestion, missingRequiredQuestion));

        assertThatThrownBy(() -> sut.submitDraft(SubmitDraftFormResponseCommand.builder()
            .formResponseId(FORM_RESPONSE_ID)
            .requesterMemberId(MEMBER_ID)
            .build()))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.REQUIRED_QUESTION_NOT_ANSWERED);
    }

    @Test
    @DisplayName("submitDraft: allowedQuestionIds 에 폼 소속 아닌 질문 있으면 QUESTION_IS_NOT_OWNED_BY_FORM")
    void submitDraft_allowedQuestionIds_교차폼_질문_거부() {
        FormResponse draft = draftResponse();
        Question formQuestion = question(10L, false);
        given(loadFormResponsePort.findById(FORM_RESPONSE_ID)).willReturn(Optional.of(draft));
        given(loadQuestionPort.listByFormId(FORM_ID)).willReturn(List.of(formQuestion));

        assertThatThrownBy(() -> sut.submitDraft(SubmitDraftFormResponseCommand.builder()
            .formResponseId(FORM_RESPONSE_ID)
            .requesterMemberId(MEMBER_ID)
            .allowedQuestionIds(Set.of(10L, 999L)) // 999L 은 폼 소속 아님
            .build()))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.QUESTION_IS_NOT_OWNED_BY_FORM);

        then(saveFormResponsePort).should(never()).save(any());
    }

    @Test
    @DisplayName("submitDraft: requiredQuestionIds 에 폼 소속 아닌 질문 있으면 QUESTION_IS_NOT_OWNED_BY_FORM")
    void submitDraft_requiredQuestionIds_교차폼_질문_거부() {
        FormResponse draft = draftResponse();
        Question formQuestion = question(10L, false);
        given(loadFormResponsePort.findById(FORM_RESPONSE_ID)).willReturn(Optional.of(draft));
        given(loadQuestionPort.listByFormId(FORM_ID)).willReturn(List.of(formQuestion));

        assertThatThrownBy(() -> sut.submitDraft(SubmitDraftFormResponseCommand.builder()
            .formResponseId(FORM_RESPONSE_ID)
            .requesterMemberId(MEMBER_ID)
            .requiredQuestionIds(Set.of(999L)) // 폼 소속 아님
            .build()))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.QUESTION_IS_NOT_OWNED_BY_FORM);

        then(saveFormResponsePort).should(never()).save(any());
    }

    @Test
    @DisplayName("submitDraft: required 가 allowed 부분집합 아니면 INVALID_SUBMIT_SCOPE")
    void submitDraft_required_allowed_부분집합_아니면_거부() {
        FormResponse draft = draftResponse();
        Question q10 = question(10L, false);
        Question q20 = question(20L, false);
        given(loadFormResponsePort.findById(FORM_RESPONSE_ID)).willReturn(Optional.of(draft));
        given(loadQuestionPort.listByFormId(FORM_ID)).willReturn(List.of(q10, q20));

        assertThatThrownBy(() -> sut.submitDraft(SubmitDraftFormResponseCommand.builder()
            .formResponseId(FORM_RESPONSE_ID)
            .requesterMemberId(MEMBER_ID)
            .allowedQuestionIds(Set.of(10L))
            .requiredQuestionIds(Set.of(10L, 20L)) // Q20 은 allowed 밖
            .build()))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.INVALID_SUBMIT_SCOPE);

        then(saveFormResponsePort).should(never()).save(any());
    }

    @Test
    @DisplayName("draft 제출 scope의 allowed question 밖에 저장된 답변이 있으면 실패한다")
    void draft_제출_scope의_allowed_question_밖에_저장된_답변이면_실패한다() {
        FormResponse draft = draftResponse();
        Question q10 = question(10L, false);
        Question hiddenQuestion = question(20L, false);
        given(loadFormResponsePort.findById(FORM_RESPONSE_ID)).willReturn(Optional.of(draft));
        given(loadAnswerPort.listByFormResponseId(FORM_RESPONSE_ID))
            .willReturn(List.of(answer(draft, hiddenQuestion)));
        given(loadQuestionPort.listByFormId(FORM_ID)).willReturn(List.of(q10, hiddenQuestion));

        assertThatThrownBy(() -> sut.submitDraft(SubmitDraftFormResponseCommand.builder()
            .formResponseId(FORM_RESPONSE_ID)
            .requesterMemberId(MEMBER_ID)
            .requiredQuestionIds(Set.of())
            .allowedQuestionIds(Set.of(10L))
            .build()))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.QUESTION_IS_NOT_OWNED_BY_FORM);

        then(saveFormResponsePort).should(never()).save(any());
    }

    @Test
    @DisplayName("submitDraft_allowedQuestionIds_중_미답변_질문에_null_answer가_저장된다")
    void submitDraft_미답변_선택_질문에_null_answer_저장() {
        // given — Q10(필수, 답변됨) + Q20(선택, 미답변)
        FormResponse draft = draftResponse();
        FormSection section = section(1L, 1L);
        Question requiredAnswered = questionInSection(10L, QuestionType.SHORT_TEXT, true, section);
        Question optionalUnanswered = questionInSection(20L, QuestionType.LONG_TEXT, false, section);
        Answer answered = answer(draft, requiredAnswered);
        ReflectionTestUtils.setField(answered, "id", 1000L);

        given(loadFormResponsePort.findById(FORM_RESPONSE_ID)).willReturn(Optional.of(draft));
        given(loadAnswerPort.listByFormResponseId(FORM_RESPONSE_ID)).willReturn(List.of(answered));
        given(loadFormSectionPort.listByFormId(FORM_ID)).willReturn(List.of(section));
        given(loadQuestionPort.listByFormId(FORM_ID))
            .willReturn(List.of(requiredAnswered, optionalUnanswered));
        given(loadQuestionPort.listByIdIn(Set.of(20L)))
            .willReturn(List.of(optionalUnanswered));

        // when
        sut.submitDraft(SubmitDraftFormResponseCommand.builder()
            .formResponseId(FORM_RESPONSE_ID)
            .requesterMemberId(MEMBER_ID)
            .requiredQuestionIds(Set.of(10L))
            .allowedQuestionIds(Set.of(10L, 20L))
            .build());

        // then — Q20에 대한 null answer가 saveAll로 저장됨
        then(loadQuestionPort).should().listByIdIn(Set.of(20L));
        then(saveAnswerPort).should().saveAll(argThat(answers ->
            answers.size() == 1 &&
            answers.get(0).getQuestion().getId().equals(20L) &&
            answers.get(0).getTextValue() == null
        ));
    }

    @Test
    @DisplayName("submitDraft_allowedQuestionIds_밖의_질문에는_null_answer가_생성되지_않는다")
    void submitDraft_allowedQuestionIds_밖_질문에_null_answer_미생성() {
        // given — allowedQuestionIds = {Q10} (파트 필터링 결과), Q20은 범위 밖
        FormResponse draft = draftResponse();
        FormSection section = section(1L, 1L);
        Question q10 = questionInSection(10L, section, true);
        Question q20 = questionInSection(20L, section, false);
        Answer answered = answer(draft, q10);
        ReflectionTestUtils.setField(answered, "id", 1000L);

        given(loadFormResponsePort.findById(FORM_RESPONSE_ID)).willReturn(Optional.of(draft));
        given(loadAnswerPort.listByFormResponseId(FORM_RESPONSE_ID)).willReturn(List.of(answered));
        given(loadFormSectionPort.listByFormId(FORM_ID)).willReturn(List.of(section));
        given(loadQuestionPort.listByFormId(FORM_ID)).willReturn(List.of(q10, q20));

        // when
        sut.submitDraft(SubmitDraftFormResponseCommand.builder()
            .formResponseId(FORM_RESPONSE_ID)
            .requesterMemberId(MEMBER_ID)
            .requiredQuestionIds(Set.of(10L))
            .allowedQuestionIds(Set.of(10L))
            .build());

        // then — Q20은 allowedQuestionIds 밖이므로 listByIdIn 호출 안 됨
        then(loadQuestionPort).should(never()).listByIdIn(any());
    }

    @Test
    @DisplayName("submitDraft_allowedQuestionIds가_모두_답변됐으면_null_answer를_저장하지_않는다")
    void submitDraft_전체_답변_완료시_null_answer_미저장() {
        // given — Q10, Q20 모두 답변됨
        FormResponse draft = draftResponse();
        FormSection section = section(1L, 1L);
        Question q10 = questionInSection(10L, section, true);
        Question q20 = questionInSection(20L, section, false);
        Answer answered10 = answer(draft, q10);
        Answer answered20 = answer(draft, q20);
        ReflectionTestUtils.setField(answered10, "id", 1000L);
        ReflectionTestUtils.setField(answered20, "id", 1001L);

        given(loadFormResponsePort.findById(FORM_RESPONSE_ID)).willReturn(Optional.of(draft));
        given(loadAnswerPort.listByFormResponseId(FORM_RESPONSE_ID))
            .willReturn(List.of(answered10, answered20));
        given(loadFormSectionPort.listByFormId(FORM_ID)).willReturn(List.of(section));
        given(loadQuestionPort.listByFormId(FORM_ID)).willReturn(List.of(q10, q20));

        // when
        sut.submitDraft(SubmitDraftFormResponseCommand.builder()
            .formResponseId(FORM_RESPONSE_ID)
            .requesterMemberId(MEMBER_ID)
            .requiredQuestionIds(Set.of(10L))
            .allowedQuestionIds(Set.of(10L, 20L))
            .build());

        // then — 미답변 질문 없으므로 listByIdIn 호출 안 함
        then(loadQuestionPort).should(never()).listByIdIn(any());
    }

    @Test
    @DisplayName("submitDraft_allowedQuestionIds가_null이면_null_answer를_저장하지_않는다")
    void submitDraft_allowedQuestionIds_null이면_null_answer_미저장() {
        // given — 비프로젝트 경로: allowedQuestionIds 없음
        FormResponse draft = draftResponse();
        FormSection section = section(1L, 1L);
        Question q = questionInSection(10L, section, true);

        given(loadFormResponsePort.findById(FORM_RESPONSE_ID)).willReturn(Optional.of(draft));
        given(loadAnswerPort.listByFormResponseId(FORM_RESPONSE_ID))
            .willReturn(List.of(answer(draft, q)));
        given(loadFormSectionPort.listByFormId(FORM_ID)).willReturn(List.of(section));
        given(loadQuestionPort.listByFormId(FORM_ID)).willReturn(List.of(q));

        // when
        sut.submitDraft(SubmitDraftFormResponseCommand.builder()
            .formResponseId(FORM_RESPONSE_ID)
            .requesterMemberId(MEMBER_ID)
            .build());

        // then — allowedQuestionIds null → early return
        then(loadQuestionPort).should(never()).listByIdIn(any());
    }

    @Test
    @DisplayName("createDraft: respondentMemberId=null 이면 RESPONDENT_MEMBER_ID_REQUIRED")
    void createDraft_respondentMemberIdNull_예외() {
        assertThatThrownBy(() -> sut.createDraft(CreateDraftFormResponseCommand.builder()
            .formId(FORM_ID)
            .respondentMemberId(null)
            .build()))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.RESPONDENT_MEMBER_ID_REQUIRED);

        then(loadFormPort).should(never()).findById(any());
        then(loadFormResponsePort).should(never()).existsByFormIdAndMemberId(any(), any());
    }

    @Test
    @DisplayName("submitImmediately: respondentMemberId=null 이면 RESPONDENT_MEMBER_ID_REQUIRED")
    void submitImmediately_respondentMemberIdNull_예외() {
        assertThatThrownBy(() -> sut.submitImmediately(SubmitFormResponseCommand.builder()
            .formId(FORM_ID)
            .respondentMemberId(null)
            .answers(List.of())
            .build()))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.RESPONDENT_MEMBER_ID_REQUIRED);

        then(loadFormPort).should(never()).findById(any());
    }

    @Test
    @DisplayName("updateResponse: respondentMemberId=null 이면 RESPONDENT_MEMBER_ID_REQUIRED")
    void updateResponse_respondentMemberIdNull_예외() {
        assertThatThrownBy(() -> sut.updateResponse(UpdateFormResponseCommand.builder()
            .formId(FORM_ID)
            .respondentMemberId(null)
            .answers(List.of())
            .build()))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.RESPONDENT_MEMBER_ID_REQUIRED);

        then(loadFormPort).should(never()).findById(any());
    }

    @Test
    @DisplayName("deleteResponse: respondentMemberId=null 이면 RESPONDENT_MEMBER_ID_REQUIRED")
    void deleteResponse_respondentMemberIdNull_예외() {
        assertThatThrownBy(() -> sut.deleteResponse(DeleteFormResponseCommand.builder()
            .formId(FORM_ID)
            .respondentMemberId(null)
            .build()))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.RESPONDENT_MEMBER_ID_REQUIRED);

        then(loadFormPort).should(never()).findById(any());
    }

    @Test
    @DisplayName("조건부 섹션 이동으로 건너뛴 섹션의 필수 질문은 검증하지 않는다")
    void 조건부_섹션_이동으로_건너뛴_섹션의_필수_질문은_검증하지_않는다() {
        // given — S1 → (O1 선택 시 S3으로 점프) → S3, S2는 건너뜀
        FormSection s1 = section(1L, 1L);
        FormSection s2 = section(2L, 2L);
        FormSection s3 = section(3L, 3L);

        Question qRadio = questionInSection(10L, QuestionType.RADIO, false, s1);
        Question qRequiredInSkipped = questionInSection(20L, QuestionType.SHORT_TEXT, true, s2);
        Question qOptional = questionInSection(30L, QuestionType.SHORT_TEXT, false, s3);

        // O1: nextSectionId = s3.id → S2 건너뜀
        QuestionOption o1 = QuestionOption.create("선택지", 1L, false, s3.getId());
        ReflectionTestUtils.setField(o1, "id", 100L);
        ReflectionTestUtils.setField(o1, "question", qRadio);

        FormResponse draft = draftResponse();
        Answer radioAnswer = answer(draft, qRadio);
        ReflectionTestUtils.setField(radioAnswer, "id", 1000L);
        Answer optionalAnswer = answer(draft, qOptional);
        ReflectionTestUtils.setField(optionalAnswer, "id", 1001L);

        given(loadFormResponsePort.findById(FORM_RESPONSE_ID)).willReturn(Optional.of(draft));
        given(loadAnswerPort.listByFormResponseId(FORM_RESPONSE_ID))
            .willReturn(List.of(radioAnswer, optionalAnswer));
        given(loadAnswerPort.listChoicesByAnswerIdIn(any())).willAnswer(inv -> {
            // radioAnswer → o1 선택
            var mockChoice = org.mockito.Mockito.mock(
                com.umc.product.form.domain.AnswerChoice.class);
            given(mockChoice.getAnswer()).willReturn(radioAnswer);
            given(mockChoice.getQuestionOption()).willReturn(o1);
            return List.of(mockChoice);
        });
        given(loadFormSectionPort.listByFormId(FORM_ID)).willReturn(List.of(s1, s2, s3));
        given(loadQuestionPort.listByFormId(FORM_ID))
            .willReturn(List.of(qRadio, qRequiredInSkipped, qOptional));
        given(loadQuestionOptionPort.listByQuestionIdIn(Set.of(qRadio.getId())))
            .willReturn(List.of(o1));

        // when — S2의 필수 질문(qRequiredInSkipped)은 미답변이지만 건너뛴 섹션이므로 예외 없음
        sut.submitDraft(SubmitDraftFormResponseCommand.builder()
            .formResponseId(FORM_RESPONSE_ID)
            .requesterMemberId(MEMBER_ID)
            .build());

        then(saveFormResponsePort).should().save(draft);
    }

    @Test
    @DisplayName("submitDraft: caller required 라도 조건부 이동으로 건너뛴 섹션의 질문은 검증하지 않는다")
    void submitDraft_caller_required_건너뛴_섹션_질문은_검증_제외() {
        // given — S1 → (O1 선택 시 S3으로 점프) → S3, S2는 건너뜀
        FormSection s1 = section(1L, 1L);
        FormSection s2 = section(2L, 2L);
        FormSection s3 = section(3L, 3L);

        Question qRadio = questionInSection(10L, QuestionType.RADIO, false, s1);
        Question qRequiredInSkipped = questionInSection(20L, QuestionType.SHORT_TEXT, true, s2);
        Question qOptional = questionInSection(30L, QuestionType.SHORT_TEXT, false, s3);

        QuestionOption o1 = QuestionOption.create("선택지", 1L, false, s3.getId());
        ReflectionTestUtils.setField(o1, "id", 100L);
        ReflectionTestUtils.setField(o1, "question", qRadio);

        FormResponse draft = draftResponse();
        Answer radioAnswer = answer(draft, qRadio);
        ReflectionTestUtils.setField(radioAnswer, "id", 1000L);

        given(loadFormResponsePort.findById(FORM_RESPONSE_ID)).willReturn(Optional.of(draft));
        given(loadAnswerPort.listByFormResponseId(FORM_RESPONSE_ID)).willReturn(List.of(radioAnswer));
        given(loadAnswerPort.listChoicesByAnswerIdIn(any())).willAnswer(inv -> {
            var mockChoice = org.mockito.Mockito.mock(
                com.umc.product.form.domain.AnswerChoice.class);
            given(mockChoice.getAnswer()).willReturn(radioAnswer);
            given(mockChoice.getQuestionOption()).willReturn(o1);
            return List.of(mockChoice);
        });
        given(loadFormSectionPort.listByFormId(FORM_ID)).willReturn(List.of(s1, s2, s3));
        given(loadQuestionPort.listByFormId(FORM_ID))
            .willReturn(List.of(qRadio, qRequiredInSkipped, qOptional));
        given(loadQuestionOptionPort.listByQuestionIdIn(Set.of(qRadio.getId())))
            .willReturn(List.of(o1));

        // when — caller 가 Q20(건너뛴 섹션 소속)을 required 로 넘겨도 건너뛴 섹션이므로 통과
        sut.submitDraft(SubmitDraftFormResponseCommand.builder()
            .formResponseId(FORM_RESPONSE_ID)
            .requesterMemberId(MEMBER_ID)
            .allowedQuestionIds(Set.of(10L, 20L, 30L))
            .requiredQuestionIds(Set.of(10L, 20L))
            .build());

        then(saveFormResponsePort).should().save(draft);
    }

    @Test
    @DisplayName("submitDraft: 방문 경로의 caller required 질문 미답변이면 REQUIRED_QUESTION_NOT_ANSWERED")
    void submitDraft_방문경로의_caller_required_미답변시_거부() {
        // given — S1 → S2 (조건부 이동 없음, 전체 방문). caller required = {Q10, Q20}. Q20 미답변.
        FormSection section = section(1L, 1L);
        Question q10 = questionInSection(10L, section, true);
        Question q20 = questionInSection(20L, section, false);

        FormResponse draft = draftResponse();
        Answer answered = answer(draft, q10);
        ReflectionTestUtils.setField(answered, "id", 1000L);

        given(loadFormResponsePort.findById(FORM_RESPONSE_ID)).willReturn(Optional.of(draft));
        given(loadAnswerPort.listByFormResponseId(FORM_RESPONSE_ID)).willReturn(List.of(answered));
        given(loadFormSectionPort.listByFormId(FORM_ID)).willReturn(List.of(section));
        given(loadQuestionPort.listByFormId(FORM_ID)).willReturn(List.of(q10, q20));

        assertThatThrownBy(() -> sut.submitDraft(SubmitDraftFormResponseCommand.builder()
            .formResponseId(FORM_RESPONSE_ID)
            .requesterMemberId(MEMBER_ID)
            .allowedQuestionIds(Set.of(10L, 20L))
            .requiredQuestionIds(Set.of(10L, 20L))
            .build()))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.REQUIRED_QUESTION_NOT_ANSWERED);
    }

    @Test
    @DisplayName("SHORT_TEXT 답변이 빈 문자열이면 저장하지 않고 INVALID_ANSWER_FORMAT으로 거부한다")
    void shortText_답변이_빈_문자열이면_INVALID_ANSWER_FORMAT() {
        FormResponse draft = draftResponse();
        Question question = question(10L, QuestionType.SHORT_TEXT, false);
        given(loadFormResponsePort.findById(FORM_RESPONSE_ID)).willReturn(Optional.of(draft));
        given(loadQuestionPort.listByFormId(FORM_ID)).willReturn(List.of(question));

        assertThatThrownBy(() -> sut.updateDraft(UpdateDraftFormResponseCommand.builder()
            .formResponseId(FORM_RESPONSE_ID)
            .requesterMemberId(MEMBER_ID)
            .answers(List.of(AnswerCommand.builder()
                .questionId(question.getId())
                .textValue("")
                .build()))
            .build()))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.INVALID_ANSWER_FORMAT);

        then(saveAnswerPort).should(never()).saveAll(any());
    }

    @Test
    @DisplayName("LONG_TEXT 답변이 공백 문자열이면 저장하지 않고 INVALID_ANSWER_FORMAT으로 거부한다")
    void longText_답변이_공백_문자열이면_INVALID_ANSWER_FORMAT() {
        FormResponse draft = draftResponse();
        Question question = question(10L, QuestionType.LONG_TEXT, false);
        given(loadFormResponsePort.findById(FORM_RESPONSE_ID)).willReturn(Optional.of(draft));
        given(loadQuestionPort.listByFormId(FORM_ID)).willReturn(List.of(question));

        assertThatThrownBy(() -> sut.updateDraft(UpdateDraftFormResponseCommand.builder()
            .formResponseId(FORM_RESPONSE_ID)
            .requesterMemberId(MEMBER_ID)
            .answers(List.of(AnswerCommand.builder()
                .questionId(question.getId())
                .textValue("   ")
                .build()))
            .build()))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.INVALID_ANSWER_FORMAT);

        then(saveAnswerPort).should(never()).saveAll(any());
    }

    // ============================================================
    //          익명 응답
    // ============================================================

    @Test
    @DisplayName("submitAnonymousImmediately: 토큰 발급 + sha256 저장 + SUBMITTED 상태 + rawKey 반환")
    void submitAnonymousImmediately_토큰_발급_및_저장() {
        String rawKey = "raw-key-example";
        String hash = "hash-of-raw-key";

        given(loadFormPort.findById(FORM_ID)).willReturn(Optional.of(publishedForm(false)));
        given(loadQuestionPort.listByFormId(FORM_ID)).willReturn(List.of());
        given(secureTokenGenerator.generateOpaqueToken()).willReturn(rawKey);
        given(secureTokenGenerator.sha256Hex(rawKey)).willReturn(hash);
        given(saveFormResponsePort.save(any(FormResponse.class))).willAnswer(invocation -> {
            FormResponse response = invocation.getArgument(0);
            ReflectionTestUtils.setField(response, "id", FORM_RESPONSE_ID);
            return response;
        });

        AnonymousFormResponseResult result = sut.submitAnonymousImmediately(
            SubmitAnonymousImmediatelyFormResponseCommand.builder()
                .formId(FORM_ID)
                .answers(List.of())
                .build()
        );

        assertThat(result.formResponseId()).isEqualTo(FORM_RESPONSE_ID);
        assertThat(result.responseAccessKey()).isEqualTo(rawKey);

        then(saveFormResponsePort).should().save(argThat(fr ->
            fr.getRespondentMemberId() == null
                && hash.equals(fr.getResponseAccessKeyHash())
                && fr.getStatus() == com.umc.product.form.domain.enums.FormResponseStatus.SUBMITTED
        ));
        // 익명은 중복 정책 검사 skip
        then(loadFormResponsePort).should(never()).existsByFormIdAndMemberId(any(), any());
    }

    @Test
    @DisplayName("updateAnonymousResponse: rawKey null 이면 RESPONSE_ACCESS_KEY_REQUIRED")
    void updateAnonymousResponse_rawKey_null_예외() {
        assertThatThrownBy(() -> sut.updateAnonymousResponse(UpdateAnonymousFormResponseCommand.builder()
            .responseAccessKey(null)
            .answers(List.of())
            .build()))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.RESPONSE_ACCESS_KEY_REQUIRED);

        then(loadFormResponsePort).should(never()).findSubmittedByAccessKeyHash(any());
    }

    @Test
    @DisplayName("updateAnonymousResponse: hash 매칭 실패면 FORM_RESPONSE_FORBIDDEN")
    void updateAnonymousResponse_hash_매칭_실패_FORBIDDEN() {
        String rawKey = "raw";
        String hash = "hash";
        given(secureTokenGenerator.sha256Hex(rawKey)).willReturn(hash);
        given(loadFormResponsePort.findSubmittedByAccessKeyHash(hash)).willReturn(Optional.empty());

        assertThatThrownBy(() -> sut.updateAnonymousResponse(UpdateAnonymousFormResponseCommand.builder()
            .responseAccessKey(rawKey)
            .answers(List.of())
            .build()))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.FORM_RESPONSE_FORBIDDEN);

        then(saveAnswerPort).should(never()).deleteAllByFormResponseId(any());
    }

    @Test
    @DisplayName("updateAnonymousResponse: 매칭된 응답이 기명이면 FORM_RESPONSE_FORBIDDEN")
    void updateAnonymousResponse_기명_응답_거부_FORBIDDEN() {
        String rawKey = "raw";
        String hash = "hash";
        FormResponse namedResponse = draftResponse(); // 기명
        given(secureTokenGenerator.sha256Hex(rawKey)).willReturn(hash);
        given(loadFormResponsePort.findSubmittedByAccessKeyHash(hash)).willReturn(Optional.of(namedResponse));

        assertThatThrownBy(() -> sut.updateAnonymousResponse(UpdateAnonymousFormResponseCommand.builder()
            .responseAccessKey(rawKey)
            .answers(List.of())
            .build()))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.FORM_RESPONSE_FORBIDDEN);

        then(saveAnswerPort).should(never()).deleteAllByFormResponseId(any());
    }

    @Test
    @DisplayName("제출 완료 익명 응답 수정은 allowed 범위 밖 질문을 거부한다")
    void updateAnonymousResponse_allowed_범위_밖_질문_거부() {
        String rawKey = "raw";
        String hash = "hash";
        FormResponse response = FormResponse.createAnonymousDraft(publishedForm(true), hash);
        ReflectionTestUtils.setField(response, "id", FORM_RESPONSE_ID);
        response.submit(Instant.parse("2026-07-15T00:00:00Z"), null);
        Question selectedTrackQuestion = question(10L, false);
        Question otherTrackQuestion = question(20L, false);
        given(secureTokenGenerator.sha256Hex(rawKey)).willReturn(hash);
        given(loadFormResponsePort.findSubmittedByAccessKeyHash(hash)).willReturn(Optional.of(response));
        given(loadQuestionPort.listByFormId(FORM_ID))
            .willReturn(List.of(selectedTrackQuestion, otherTrackQuestion));

        assertThatThrownBy(() -> sut.updateAnonymousResponse(UpdateAnonymousFormResponseCommand.builder()
            .responseAccessKey(rawKey)
            .answers(List.of(AnswerCommand.builder()
                .questionId(otherTrackQuestion.getId())
                .textValue("다른 트랙 답변")
                .build()))
            .allowedQuestionIds(Set.of(selectedTrackQuestion.getId()))
            .requiredQuestionIds(Set.of())
            .build()))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.QUESTION_IS_NOT_OWNED_BY_FORM);

        then(saveAnswerPort).should(never()).deleteAllByFormResponseId(any());
    }

    @Test
    @DisplayName("deleteAnonymousResponse: rawKey null 이면 RESPONSE_ACCESS_KEY_REQUIRED")
    void deleteAnonymousResponse_rawKey_null_예외() {
        assertThatThrownBy(() -> sut.deleteAnonymousResponse(DeleteAnonymousFormResponseCommand.builder()
            .responseAccessKey(null)
            .build()))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.RESPONSE_ACCESS_KEY_REQUIRED);

        then(loadFormResponsePort).should(never()).findSubmittedByAccessKeyHash(any());
    }

    @Test
    @DisplayName("deleteAnonymousResponse: hash 매칭 실패면 FORM_RESPONSE_FORBIDDEN")
    void deleteAnonymousResponse_hash_매칭_실패_FORBIDDEN() {
        String rawKey = "raw";
        String hash = "hash";
        given(secureTokenGenerator.sha256Hex(rawKey)).willReturn(hash);
        given(loadFormResponsePort.findSubmittedByAccessKeyHash(hash)).willReturn(Optional.empty());

        assertThatThrownBy(() -> sut.deleteAnonymousResponse(DeleteAnonymousFormResponseCommand.builder()
            .responseAccessKey(rawKey)
            .build()))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.FORM_RESPONSE_FORBIDDEN);

        then(saveFormResponsePort).should(never()).deleteById(any());
    }

    @Test
    @DisplayName("deleteAnonymousResponse: 매칭된 응답이 기명이면 FORM_RESPONSE_FORBIDDEN")
    void deleteAnonymousResponse_기명_응답_거부_FORBIDDEN() {
        String rawKey = "raw";
        String hash = "hash";
        FormResponse namedResponse = draftResponse();
        given(secureTokenGenerator.sha256Hex(rawKey)).willReturn(hash);
        given(loadFormResponsePort.findSubmittedByAccessKeyHash(hash)).willReturn(Optional.of(namedResponse));

        assertThatThrownBy(() -> sut.deleteAnonymousResponse(DeleteAnonymousFormResponseCommand.builder()
            .responseAccessKey(rawKey)
            .build()))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.FORM_RESPONSE_FORBIDDEN);

        then(saveFormResponsePort).should(never()).deleteById(any());
    }

    @Test
    @DisplayName("createAnonymousDraft: opaque token 발급 + sha256 저장 + rawKey 반환")
    void createAnonymousDraft_토큰_발급_및_저장() {
        String rawKey = "raw-key-example";
        String hash = "hash-of-raw-key";

        given(loadFormPort.findById(FORM_ID)).willReturn(Optional.of(publishedForm(false)));
        given(secureTokenGenerator.generateOpaqueToken()).willReturn(rawKey);
        given(secureTokenGenerator.sha256Hex(rawKey)).willReturn(hash);
        given(saveFormResponsePort.save(any(FormResponse.class))).willAnswer(invocation -> {
            FormResponse response = invocation.getArgument(0);
            ReflectionTestUtils.setField(response, "id", FORM_RESPONSE_ID);
            return response;
        });

        AnonymousFormResponseResult result = sut.createAnonymousDraft(
            CreateAnonymousDraftFormResponseCommand.builder()
                .formId(FORM_ID)
                .build()
        );

        assertThat(result.formResponseId()).isEqualTo(FORM_RESPONSE_ID);
        assertThat(result.responseAccessKey()).isEqualTo(rawKey);

        then(saveFormResponsePort).should().save(argThat(fr ->
            fr.getRespondentMemberId() == null
                && hash.equals(fr.getResponseAccessKeyHash())
        ));
        // 익명은 중복 정책 검사 skip
        then(loadFormResponsePort).should(never()).existsByFormIdAndMemberId(any(), any());
    }

    @Test
    @DisplayName("updateAnonymousDraft: rawKey null 이면 RESPONSE_ACCESS_KEY_REQUIRED")
    void updateAnonymousDraft_rawKey_null_예외() {
        assertThatThrownBy(() -> sut.updateAnonymousDraft(UpdateAnonymousDraftFormResponseCommand.builder()
            .responseAccessKey(null)
            .answers(List.of())
            .build()))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.RESPONSE_ACCESS_KEY_REQUIRED);

        then(loadFormResponsePort).should(never()).findDraftByAccessKeyHash(any());
    }

    @Test
    @DisplayName("updateAnonymousDraft: hash 매칭 실패면 FORM_RESPONSE_FORBIDDEN")
    void updateAnonymousDraft_hash_매칭_실패_FORBIDDEN() {
        String rawKey = "raw";
        String hash = "hash";
        given(secureTokenGenerator.sha256Hex(rawKey)).willReturn(hash);
        given(loadFormResponsePort.findDraftByAccessKeyHash(hash)).willReturn(Optional.empty());

        assertThatThrownBy(() -> sut.updateAnonymousDraft(UpdateAnonymousDraftFormResponseCommand.builder()
            .responseAccessKey(rawKey)
            .answers(List.of())
            .build()))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.FORM_RESPONSE_FORBIDDEN);

        then(saveAnswerPort).should(never()).deleteAllByFormResponseId(any());
    }

    @Test
    @DisplayName("updateAnonymousDraft: 매칭된 draft 가 기명이면 FORM_RESPONSE_FORBIDDEN")
    void updateAnonymousDraft_기명_draft_거부_FORBIDDEN() {
        String rawKey = "raw";
        String hash = "hash";
        FormResponse namedDraft = draftResponse(); // 기명 (MEMBER_ID)
        given(secureTokenGenerator.sha256Hex(rawKey)).willReturn(hash);
        given(loadFormResponsePort.findDraftByAccessKeyHash(hash)).willReturn(Optional.of(namedDraft));

        assertThatThrownBy(() -> sut.updateAnonymousDraft(UpdateAnonymousDraftFormResponseCommand.builder()
            .responseAccessKey(rawKey)
            .answers(List.of())
            .build()))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.FORM_RESPONSE_FORBIDDEN);

        then(saveAnswerPort).should(never()).deleteAllByFormResponseId(any());
    }

    @Test
    @DisplayName("submitAnonymousDraft: rawKey null 이면 RESPONSE_ACCESS_KEY_REQUIRED")
    void submitAnonymousDraft_rawKey_null_예외() {
        assertThatThrownBy(() -> sut.submitAnonymousDraft(SubmitAnonymousDraftFormResponseCommand.builder()
            .responseAccessKey(null)
            .build()))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.RESPONSE_ACCESS_KEY_REQUIRED);

        then(loadFormResponsePort).should(never()).findDraftByAccessKeyHash(any());
    }

    @Test
    @DisplayName("submitAnonymousDraft: hash 매칭 실패면 FORM_RESPONSE_FORBIDDEN")
    void submitAnonymousDraft_hash_매칭_실패_FORBIDDEN() {
        String rawKey = "raw";
        String hash = "hash";
        given(secureTokenGenerator.sha256Hex(rawKey)).willReturn(hash);
        given(loadFormResponsePort.findDraftByAccessKeyHash(hash)).willReturn(Optional.empty());

        assertThatThrownBy(() -> sut.submitAnonymousDraft(SubmitAnonymousDraftFormResponseCommand.builder()
            .responseAccessKey(rawKey)
            .build()))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.FORM_RESPONSE_FORBIDDEN);

        then(saveFormResponsePort).should(never()).save(any());
    }

    @Test
    @DisplayName("submitAnonymousDraft: 매칭된 draft 가 기명이면 FORM_RESPONSE_FORBIDDEN")
    void submitAnonymousDraft_기명_draft_거부_FORBIDDEN() {
        String rawKey = "raw";
        String hash = "hash";
        FormResponse namedDraft = draftResponse();
        given(secureTokenGenerator.sha256Hex(rawKey)).willReturn(hash);
        given(loadFormResponsePort.findDraftByAccessKeyHash(hash)).willReturn(Optional.of(namedDraft));

        assertThatThrownBy(() -> sut.submitAnonymousDraft(SubmitAnonymousDraftFormResponseCommand.builder()
            .responseAccessKey(rawKey)
            .build()))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.FORM_RESPONSE_FORBIDDEN);

        then(saveFormResponsePort).should(never()).save(any());
    }

    @Test
    @DisplayName("deleteAnonymousDraft: rawKey null 이면 RESPONSE_ACCESS_KEY_REQUIRED")
    void deleteAnonymousDraft_rawKey_null_예외() {
        assertThatThrownBy(() -> sut.deleteAnonymousDraft(DeleteAnonymousDraftFormResponseCommand.builder()
            .responseAccessKey(null)
            .build()))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.RESPONSE_ACCESS_KEY_REQUIRED);

        then(loadFormResponsePort).should(never()).findDraftByAccessKeyHash(any());
    }

    @Test
    @DisplayName("deleteAnonymousDraft: hash 매칭 실패면 FORM_RESPONSE_FORBIDDEN")
    void deleteAnonymousDraft_hash_매칭_실패_FORBIDDEN() {
        String rawKey = "raw";
        String hash = "hash";
        given(secureTokenGenerator.sha256Hex(rawKey)).willReturn(hash);
        given(loadFormResponsePort.findDraftByAccessKeyHash(hash)).willReturn(Optional.empty());

        assertThatThrownBy(() -> sut.deleteAnonymousDraft(DeleteAnonymousDraftFormResponseCommand.builder()
            .responseAccessKey(rawKey)
            .build()))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.FORM_RESPONSE_FORBIDDEN);

        then(saveFormResponsePort).should(never()).deleteById(any());
    }

    @Test
    @DisplayName("deleteAnonymousDraft: 매칭된 draft 가 기명이면 FORM_RESPONSE_FORBIDDEN")
    void deleteAnonymousDraft_기명_draft_거부_FORBIDDEN() {
        String rawKey = "raw";
        String hash = "hash";
        FormResponse namedDraft = draftResponse();
        given(secureTokenGenerator.sha256Hex(rawKey)).willReturn(hash);
        given(loadFormResponsePort.findDraftByAccessKeyHash(hash)).willReturn(Optional.of(namedDraft));

        assertThatThrownBy(() -> sut.deleteAnonymousDraft(DeleteAnonymousDraftFormResponseCommand.builder()
            .responseAccessKey(rawKey)
            .build()))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.FORM_RESPONSE_FORBIDDEN);

        then(saveFormResponsePort).should(never()).deleteById(any());
    }

    // ============================================================
    //          Draft 조작 owner 검증 (Phase 2)
    // ============================================================

    @Test
    @DisplayName("updateDraft: requesterMemberId=null 이면 FORM_RESPONSE_FORBIDDEN (auth 계층에서 걸러졌어야 하는 방어)")
    void updateDraft_requesterMemberIdNull_예외() {
        FormResponse draft = draftResponse();
        given(loadFormResponsePort.findById(FORM_RESPONSE_ID)).willReturn(Optional.of(draft));

        assertThatThrownBy(() -> sut.updateDraft(UpdateDraftFormResponseCommand.builder()
            .formResponseId(FORM_RESPONSE_ID)
            .requesterMemberId(null)
            .answers(List.of())
            .build()))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.FORM_RESPONSE_FORBIDDEN);

        then(saveAnswerPort).should(never()).deleteAllByFormResponseId(any());
    }

    @Test
    @DisplayName("updateDraft: draft 가 익명(respondentMemberId=null) 이면 FORM_RESPONSE_FORBIDDEN")
    void updateDraft_익명_draft_는_기명_usecase_에서_거부() {
        FormResponse anonymousDraft = FormResponse.createDraft(publishedForm(true), null);
        ReflectionTestUtils.setField(anonymousDraft, "id", FORM_RESPONSE_ID);
        given(loadFormResponsePort.findById(FORM_RESPONSE_ID)).willReturn(Optional.of(anonymousDraft));

        assertThatThrownBy(() -> sut.updateDraft(UpdateDraftFormResponseCommand.builder()
            .formResponseId(FORM_RESPONSE_ID)
            .requesterMemberId(MEMBER_ID)
            .answers(List.of())
            .build()))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.FORM_RESPONSE_FORBIDDEN);

        then(saveAnswerPort).should(never()).deleteAllByFormResponseId(any());
    }

    @Test
    @DisplayName("updateDraft: 요청자가 draft 소유자가 아니면 FORM_RESPONSE_FORBIDDEN")
    void updateDraft_요청자가_소유자가_아니면_FORBIDDEN() {
        FormResponse draft = draftResponse(); // 소유자 = MEMBER_ID
        given(loadFormResponsePort.findById(FORM_RESPONSE_ID)).willReturn(Optional.of(draft));

        assertThatThrownBy(() -> sut.updateDraft(UpdateDraftFormResponseCommand.builder()
            .formResponseId(FORM_RESPONSE_ID)
            .requesterMemberId(MEMBER_ID + 1) // 다른 사용자
            .answers(List.of())
            .build()))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.FORM_RESPONSE_FORBIDDEN);

        then(saveAnswerPort).should(never()).deleteAllByFormResponseId(any());
    }

    @Test
    @DisplayName("submitDraft: requesterMemberId=null 이면 FORM_RESPONSE_FORBIDDEN (auth 계층에서 걸러졌어야 하는 방어)")
    void submitDraft_requesterMemberIdNull_예외() {
        FormResponse draft = draftResponse();
        given(loadFormResponsePort.findById(FORM_RESPONSE_ID)).willReturn(Optional.of(draft));

        assertThatThrownBy(() -> sut.submitDraft(SubmitDraftFormResponseCommand.builder()
            .formResponseId(FORM_RESPONSE_ID)
            .requesterMemberId(null)
            .build()))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.FORM_RESPONSE_FORBIDDEN);

        then(saveFormResponsePort).should(never()).save(any());
    }

    @Test
    @DisplayName("submitDraft: 요청자가 draft 소유자가 아니면 FORM_RESPONSE_FORBIDDEN")
    void submitDraft_요청자가_소유자가_아니면_FORBIDDEN() {
        FormResponse draft = draftResponse();
        given(loadFormResponsePort.findById(FORM_RESPONSE_ID)).willReturn(Optional.of(draft));

        assertThatThrownBy(() -> sut.submitDraft(SubmitDraftFormResponseCommand.builder()
            .formResponseId(FORM_RESPONSE_ID)
            .requesterMemberId(MEMBER_ID + 1)
            .build()))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.FORM_RESPONSE_FORBIDDEN);

        then(saveFormResponsePort).should(never()).save(any());
    }

    @Test
    @DisplayName("deleteDraft: requesterMemberId=null 이면 FORM_RESPONSE_FORBIDDEN (auth 계층에서 걸러졌어야 하는 방어)")
    void deleteDraft_requesterMemberIdNull_예외() {
        FormResponse draft = draftResponse();
        given(loadFormResponsePort.findById(FORM_RESPONSE_ID)).willReturn(Optional.of(draft));

        assertThatThrownBy(() -> sut.deleteDraft(DeleteDraftFormResponseCommand.builder()
            .formResponseId(FORM_RESPONSE_ID)
            .requesterMemberId(null)
            .build()))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.FORM_RESPONSE_FORBIDDEN);

        then(saveFormResponsePort).should(never()).deleteById(any());
    }

    @Test
    @DisplayName("deleteDraft: 요청자가 draft 소유자가 아니면 FORM_RESPONSE_FORBIDDEN")
    void deleteDraft_요청자가_소유자가_아니면_FORBIDDEN() {
        FormResponse draft = draftResponse();
        given(loadFormResponsePort.findById(FORM_RESPONSE_ID)).willReturn(Optional.of(draft));

        assertThatThrownBy(() -> sut.deleteDraft(DeleteDraftFormResponseCommand.builder()
            .formResponseId(FORM_RESPONSE_ID)
            .requesterMemberId(MEMBER_ID + 1)
            .build()))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.FORM_RESPONSE_FORBIDDEN);

        then(saveFormResponsePort).should(never()).deleteById(any());
    }

    // ============================================================
    //          방문 경로 밖 섹션의 orphan answer 자동 정리
    // ============================================================

    @Test
    @DisplayName("submitImmediately: 방문 경로 밖 섹션의 답변은 SUBMITTED 결과에 포함되지 않는다")
    void submitImmediately_방문경로_밖_답변은_저장되지_않는다() {
        // given — S1(RADIO Q10 → S3 점프), S2(Q20), S3(Q30). S2 는 건너뜀.
        FormSection s1 = section(1L, 1L);
        FormSection s2 = section(2L, 2L);
        FormSection s3 = section(3L, 3L);
        Question qRadio = questionInSection(10L, QuestionType.RADIO, false, s1);
        Question qInSkipped = questionInSection(20L, QuestionType.SHORT_TEXT, false, s2);
        Question qOptional = questionInSection(30L, QuestionType.SHORT_TEXT, false, s3);
        QuestionOption o1 = QuestionOption.create("선택지", 1L, false, s3.getId());
        ReflectionTestUtils.setField(o1, "id", 100L);
        ReflectionTestUtils.setField(o1, "question", qRadio);

        given(loadFormPort.findById(FORM_ID)).willReturn(Optional.of(publishedForm(false)));
        given(loadFormResponsePort.existsByFormIdAndMemberId(FORM_ID, MEMBER_ID)).willReturn(false);
        given(loadFormSectionPort.listByFormId(FORM_ID)).willReturn(List.of(s1, s2, s3));
        given(loadQuestionPort.listByFormId(FORM_ID))
            .willReturn(List.of(qRadio, qInSkipped, qOptional));
        given(loadQuestionOptionPort.listByQuestionIdIn(Set.of(qRadio.getId())))
            .willReturn(List.of(o1));
        given(loadQuestionOptionPort.listByQuestionId(qRadio.getId())).willReturn(List.of(o1));
        given(loadQuestionOptionPort.existsByIdAndQuestionId(100L, qRadio.getId())).willReturn(true);
        given(saveFormResponsePort.save(any(FormResponse.class))).willAnswer(inv -> {
            FormResponse fr = inv.getArgument(0);
            ReflectionTestUtils.setField(fr, "id", FORM_RESPONSE_ID);
            return fr;
        });
        given(saveAnswerPort.saveAll(any())).willAnswer(inv -> inv.getArgument(0));

        // when — S2 소속 질문(Q20)에 답변을 함께 제출하지만 방문 경로 밖이므로 조용히 폐기돼야 함
        sut.submitImmediately(SubmitFormResponseCommand.builder()
            .formId(FORM_ID)
            .respondentMemberId(MEMBER_ID)
            .answers(List.of(
                AnswerCommand.builder().questionId(10L).selectedOptionIds(List.of(100L)).build(),
                AnswerCommand.builder().questionId(20L).textValue("건너뛴 섹션 답변").build(),
                AnswerCommand.builder().questionId(30L).textValue("경로 상 답변").build()
            ))
            .build());

        // then — Q20 은 orphan 이라 saveAll 로 전달된 answer 목록에서 제외됨
        then(saveAnswerPort).should().saveAll(argThat(answers ->
            answers.size() == 2
                && answers.stream().map(a -> a.getQuestion().getId())
                .collect(java.util.stream.Collectors.toSet()).equals(Set.of(10L, 30L))
        ));
    }

    @Test
    @DisplayName("submitAnonymousImmediately: 방문 경로 밖 섹션의 답변은 SUBMITTED 결과에 포함되지 않는다")
    void submitAnonymousImmediately_방문경로_밖_답변은_저장되지_않는다() {
        FormSection s1 = section(1L, 1L);
        FormSection s2 = section(2L, 2L);
        FormSection s3 = section(3L, 3L);
        Question qRadio = questionInSection(10L, QuestionType.RADIO, false, s1);
        Question qInSkipped = questionInSection(20L, QuestionType.SHORT_TEXT, false, s2);
        Question qOptional = questionInSection(30L, QuestionType.SHORT_TEXT, false, s3);
        QuestionOption o1 = QuestionOption.create("선택지", 1L, false, s3.getId());
        ReflectionTestUtils.setField(o1, "id", 100L);
        ReflectionTestUtils.setField(o1, "question", qRadio);

        given(loadFormPort.findById(FORM_ID)).willReturn(Optional.of(publishedForm(false)));
        given(loadFormSectionPort.listByFormId(FORM_ID)).willReturn(List.of(s1, s2, s3));
        given(loadQuestionPort.listByFormId(FORM_ID))
            .willReturn(List.of(qRadio, qInSkipped, qOptional));
        given(loadQuestionOptionPort.listByQuestionIdIn(Set.of(qRadio.getId())))
            .willReturn(List.of(o1));
        given(loadQuestionOptionPort.listByQuestionId(qRadio.getId())).willReturn(List.of(o1));
        given(loadQuestionOptionPort.existsByIdAndQuestionId(100L, qRadio.getId())).willReturn(true);
        given(secureTokenGenerator.generateOpaqueToken()).willReturn("raw-key");
        given(secureTokenGenerator.sha256Hex("raw-key")).willReturn("hash");
        given(saveFormResponsePort.save(any(FormResponse.class))).willAnswer(inv -> {
            FormResponse fr = inv.getArgument(0);
            ReflectionTestUtils.setField(fr, "id", FORM_RESPONSE_ID);
            return fr;
        });
        given(saveAnswerPort.saveAll(any())).willAnswer(inv -> inv.getArgument(0));

        sut.submitAnonymousImmediately(SubmitAnonymousImmediatelyFormResponseCommand.builder()
            .formId(FORM_ID)
            .answers(List.of(
                AnswerCommand.builder().questionId(10L).selectedOptionIds(List.of(100L)).build(),
                AnswerCommand.builder().questionId(20L).textValue("건너뛴 섹션 답변").build(),
                AnswerCommand.builder().questionId(30L).textValue("경로 상 답변").build()
            ))
            .build());

        then(saveAnswerPort).should().saveAll(argThat(answers ->
            answers.size() == 2
                && answers.stream().map(a -> a.getQuestion().getId())
                .collect(java.util.stream.Collectors.toSet()).equals(Set.of(10L, 30L))
        ));
    }

    @Test
    @DisplayName("submitDraft: 방문 경로 밖 섹션에 draft 로 저장된 답변은 orphan 정리로 삭제된다")
    void submitDraft_방문경로_밖_orphan_answer_는_삭제된다() {
        // given — S1(RADIO Q10 → S3), S2(Q20 orphan), S3(Q30). draft 에 Q20 답변이 저장돼 있음.
        FormSection s1 = section(1L, 1L);
        FormSection s2 = section(2L, 2L);
        FormSection s3 = section(3L, 3L);
        Question qRadio = questionInSection(10L, QuestionType.RADIO, false, s1);
        Question qInSkipped = questionInSection(20L, QuestionType.SHORT_TEXT, false, s2);
        Question qOptional = questionInSection(30L, QuestionType.SHORT_TEXT, false, s3);
        QuestionOption o1 = QuestionOption.create("선택지", 1L, false, s3.getId());
        ReflectionTestUtils.setField(o1, "id", 100L);
        ReflectionTestUtils.setField(o1, "question", qRadio);

        FormResponse draft = draftResponse();
        Answer radioAnswer = answer(draft, qRadio);
        Answer orphanAnswer = answer(draft, qInSkipped);
        Answer onPathAnswer = answer(draft, qOptional);
        ReflectionTestUtils.setField(radioAnswer, "id", 1000L);
        ReflectionTestUtils.setField(orphanAnswer, "id", 1001L);
        ReflectionTestUtils.setField(onPathAnswer, "id", 1002L);

        given(loadFormResponsePort.findById(FORM_RESPONSE_ID)).willReturn(Optional.of(draft));
        given(loadAnswerPort.listByFormResponseId(FORM_RESPONSE_ID))
            .willReturn(List.of(radioAnswer, orphanAnswer, onPathAnswer));
        given(loadAnswerPort.listChoicesByAnswerIdIn(any())).willAnswer(inv -> {
            var mockChoice = org.mockito.Mockito.mock(
                com.umc.product.form.domain.AnswerChoice.class);
            given(mockChoice.getAnswer()).willReturn(radioAnswer);
            given(mockChoice.getQuestionOption()).willReturn(o1);
            return List.of(mockChoice);
        });
        given(loadFormSectionPort.listByFormId(FORM_ID)).willReturn(List.of(s1, s2, s3));
        given(loadQuestionPort.listByFormId(FORM_ID))
            .willReturn(List.of(qRadio, qInSkipped, qOptional));
        given(loadQuestionOptionPort.listByQuestionIdIn(Set.of(qRadio.getId())))
            .willReturn(List.of(o1));

        // when
        sut.submitDraft(SubmitDraftFormResponseCommand.builder()
            .formResponseId(FORM_RESPONSE_ID)
            .requesterMemberId(MEMBER_ID)
            .build());

        // then — orphan Q20 답변만 삭제 대상으로 지정됨
        then(saveAnswerPort).should()
            .deleteByFormResponseIdAndQuestionIdIn(FORM_RESPONSE_ID, Set.of(20L));
        then(saveFormResponsePort).should().save(draft);
    }

    @Test
    @DisplayName("submitAnonymousDraft: 방문 경로 밖 섹션에 draft 로 저장된 답변은 orphan 정리로 삭제된다")
    void submitAnonymousDraft_방문경로_밖_orphan_answer_는_삭제된다() {
        FormSection s1 = section(1L, 1L);
        FormSection s2 = section(2L, 2L);
        FormSection s3 = section(3L, 3L);
        Question qRadio = questionInSection(10L, QuestionType.RADIO, false, s1);
        Question qInSkipped = questionInSection(20L, QuestionType.SHORT_TEXT, false, s2);
        Question qOptional = questionInSection(30L, QuestionType.SHORT_TEXT, false, s3);
        QuestionOption o1 = QuestionOption.create("선택지", 1L, false, s3.getId());
        ReflectionTestUtils.setField(o1, "id", 100L);
        ReflectionTestUtils.setField(o1, "question", qRadio);

        FormResponse anonymousDraft = FormResponse.createAnonymousDraft(publishedForm(true), "hash");
        ReflectionTestUtils.setField(anonymousDraft, "id", FORM_RESPONSE_ID);

        Answer radioAnswer = answer(anonymousDraft, qRadio);
        Answer orphanAnswer = answer(anonymousDraft, qInSkipped);
        Answer onPathAnswer = answer(anonymousDraft, qOptional);
        ReflectionTestUtils.setField(radioAnswer, "id", 1000L);
        ReflectionTestUtils.setField(orphanAnswer, "id", 1001L);
        ReflectionTestUtils.setField(onPathAnswer, "id", 1002L);

        String rawKey = "raw";
        given(secureTokenGenerator.sha256Hex(rawKey)).willReturn("hash");
        given(loadFormResponsePort.findDraftByAccessKeyHash("hash"))
            .willReturn(Optional.of(anonymousDraft));
        given(loadAnswerPort.listByFormResponseId(FORM_RESPONSE_ID))
            .willReturn(List.of(radioAnswer, orphanAnswer, onPathAnswer));
        given(loadAnswerPort.listChoicesByAnswerIdIn(any())).willAnswer(inv -> {
            var mockChoice = org.mockito.Mockito.mock(
                com.umc.product.form.domain.AnswerChoice.class);
            given(mockChoice.getAnswer()).willReturn(radioAnswer);
            given(mockChoice.getQuestionOption()).willReturn(o1);
            return List.of(mockChoice);
        });
        given(loadFormSectionPort.listByFormId(FORM_ID)).willReturn(List.of(s1, s2, s3));
        given(loadQuestionPort.listByFormId(FORM_ID))
            .willReturn(List.of(qRadio, qInSkipped, qOptional));
        given(loadQuestionOptionPort.listByQuestionIdIn(Set.of(qRadio.getId())))
            .willReturn(List.of(o1));

        sut.submitAnonymousDraft(SubmitAnonymousDraftFormResponseCommand.builder()
            .responseAccessKey(rawKey)
            .build());

        then(saveAnswerPort).should()
            .deleteByFormResponseIdAndQuestionIdIn(FORM_RESPONSE_ID, Set.of(20L));
        then(saveFormResponsePort).should().save(anonymousDraft);
    }

    @Test
    @DisplayName("submitDraft: allowedQuestionIds 에 orphan 질문이 있어도 빈 answer 로 저장되지 않는다")
    void submitDraft_allowed_안의_orphan_질문은_빈_answer_로_저장되지_않는다() {
        // given — S1(RADIO Q10 → S3), S2(Q20 orphan, allowed), S3(Q30, allowed).
        // allowedQuestionIds 에 Q20 이 있어도 skipped 섹션이라 빈 answer 저장 대상에서 제외돼야 함.
        FormSection s1 = section(1L, 1L);
        FormSection s2 = section(2L, 2L);
        FormSection s3 = section(3L, 3L);
        Question qRadio = questionInSection(10L, QuestionType.RADIO, false, s1);
        Question qInSkipped = questionInSection(20L, QuestionType.SHORT_TEXT, false, s2);
        Question qOptional = questionInSection(30L, QuestionType.SHORT_TEXT, false, s3);
        QuestionOption o1 = QuestionOption.create("선택지", 1L, false, s3.getId());
        ReflectionTestUtils.setField(o1, "id", 100L);
        ReflectionTestUtils.setField(o1, "question", qRadio);

        FormResponse draft = draftResponse();
        Answer radioAnswer = answer(draft, qRadio);
        ReflectionTestUtils.setField(radioAnswer, "id", 1000L);

        given(loadFormResponsePort.findById(FORM_RESPONSE_ID)).willReturn(Optional.of(draft));
        given(loadAnswerPort.listByFormResponseId(FORM_RESPONSE_ID))
            .willReturn(List.of(radioAnswer));
        given(loadAnswerPort.listChoicesByAnswerIdIn(any())).willAnswer(inv -> {
            var mockChoice = org.mockito.Mockito.mock(
                com.umc.product.form.domain.AnswerChoice.class);
            given(mockChoice.getAnswer()).willReturn(radioAnswer);
            given(mockChoice.getQuestionOption()).willReturn(o1);
            return List.of(mockChoice);
        });
        given(loadFormSectionPort.listByFormId(FORM_ID)).willReturn(List.of(s1, s2, s3));
        given(loadQuestionPort.listByFormId(FORM_ID))
            .willReturn(List.of(qRadio, qInSkipped, qOptional));
        given(loadQuestionOptionPort.listByQuestionIdIn(Set.of(qRadio.getId())))
            .willReturn(List.of(o1));
        given(loadQuestionPort.listByIdIn(Set.of(30L))).willReturn(List.of(qOptional));

        // when
        sut.submitDraft(SubmitDraftFormResponseCommand.builder()
            .formResponseId(FORM_RESPONSE_ID)
            .requesterMemberId(MEMBER_ID)
            .allowedQuestionIds(Set.of(10L, 20L, 30L))
            .build());

        // then — 빈 answer 저장 대상은 방문 경로 상 미답변 질문(Q30) 뿐. Q20 은 orphan 이라 제외.
        then(loadQuestionPort).should().listByIdIn(Set.of(30L));
        then(saveAnswerPort).should().saveAll(argThat(answers ->
            answers.size() == 1 && answers.get(0).getQuestion().getId().equals(30L)
        ));
    }

    private Form publishedForm(boolean allowDuplicateResponses) {
        Form form = Form.createDraft("프로젝트 지원서", 1L, allowDuplicateResponses);
        ReflectionTestUtils.setField(form, "id", FORM_ID);
        form.publish();
        return form;
    }

    private FormResponse draftResponse() {
        FormResponse response = FormResponse.createDraft(publishedForm(true), MEMBER_ID);
        ReflectionTestUtils.setField(response, "id", FORM_RESPONSE_ID);
        return response;
    }

    private FormSection section(Long id, Long orderNo) {
        FormSection section = FormSection.create(publishedForm(true), "섹션", null, orderNo);
        ReflectionTestUtils.setField(section, "id", id);
        return section;
    }

    private Question question(Long questionId, boolean isRequired) {
        return question(questionId, QuestionType.SHORT_TEXT, isRequired);
    }

    private Question question(Long questionId, QuestionType type, boolean isRequired) {
        Question question = Question.create("질문", type, isRequired, 1L);
        ReflectionTestUtils.setField(question, "id", questionId);
        return question;
    }

    private Question questionInSection(Long id, FormSection section, boolean isRequired) {
        return questionInSection(id, QuestionType.SHORT_TEXT, isRequired, section);
    }

    private Question questionInSection(Long id, QuestionType type, boolean isRequired, FormSection section) {
        Question q = question(id, type, isRequired);
        q.assignTo(section);
        return q;
    }

    private Answer answer(FormResponse formResponse, Question question) {
        return Answer.create(formResponse, question, QuestionType.SHORT_TEXT, "답변", null);
    }
}
