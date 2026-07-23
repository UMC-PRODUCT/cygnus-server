package com.umc.product.form.application.service.command;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.audit.application.port.in.annotation.Audited;
import com.umc.product.audit.domain.AuditAction;
import com.umc.product.authentication.application.service.SecureTokenGenerator;
import com.umc.product.form.application.port.in.command.ManageFormResponseUseCase;
import com.umc.product.form.application.port.in.command.dto.AnonymousFormResponseResult;
import com.umc.product.form.application.port.in.command.dto.AnswerCommand;
import com.umc.product.form.application.port.in.command.dto.ClaimAnonymousFormResponseCommand;
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
import com.umc.product.form.domain.AnswerChoice;
import com.umc.product.form.domain.Form;
import com.umc.product.form.domain.FormResponse;
import com.umc.product.form.domain.FormSection;
import com.umc.product.form.domain.Question;
import com.umc.product.form.domain.QuestionOption;
import com.umc.product.form.domain.enums.FormResponseStatus;
import com.umc.product.form.domain.enums.QuestionType;
import com.umc.product.form.domain.exception.DraftSchemaMismatchException;
import com.umc.product.form.domain.exception.FormDomainException;
import com.umc.product.form.domain.exception.FormErrorCode;
import com.umc.product.global.exception.constant.Domain;
import com.umc.product.storage.application.port.in.query.GetFileUseCase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class FormResponseCommandService implements ManageFormResponseUseCase {

    private final LoadFormPort loadFormPort;
    private final LoadFormSectionPort loadFormSectionPort;
    private final LoadQuestionPort loadQuestionPort;
    private final LoadQuestionOptionPort loadQuestionOptionPort;
    private final LoadFormResponsePort loadFormResponsePort;
    private final LoadAnswerPort loadAnswerPort;
    private final SaveFormResponsePort saveFormResponsePort;
    private final SaveAnswerPort saveAnswerPort;
    private final GetFileUseCase getFileUseCase;
    // authentication 도메인의 공용 crypto util 재사용 (SSO Auth Code 발급과 동일 패턴).
    // 재배치(common/security 등) 는 별도 리팩터 PR 대상.
    private final SecureTokenGenerator secureTokenGenerator;

    @Audited(
        domain = Domain.FORM,
        action = AuditAction.SUBMIT,
        targetType = "FormResponse",
        targetId = "#result",
        description = "'설문 응답을 제출했습니다.'"
    )
    @Override
    public Long submitImmediately(SubmitFormResponseCommand command) {
        requireRespondentMemberId(command.respondentMemberId());
        Form form = loadPublishedForm(command.formId());

        validateDuplicateResponsePolicy(form, command.respondentMemberId());

        validateAnswers(command.formId(), command.answers());
        Set<Long> visitedSectionIds = resolveVisitedSectionIds(
            command.formId(),
            extractSingleSelectedOptionIds(command.answers())
        );
        validateAllRequiredAnsweredWithVisited(
            command.formId(),
            extractQuestionIds(command.answers()),
            visitedSectionIds,
            null
        );

        // : 방문 경로 밖 섹션의 답변은 SUBMITTED 결과에 포함하지 않도록 조용히 폐기한다.
        List<AnswerCommand> answersOnPath = filterAnswersOnVisitedPath(
            command.formId(),
            command.answers(),
            visitedSectionIds
        );

        FormResponse response = FormResponse.createDraft(form, command.respondentMemberId());
        response.submit(Instant.now(), null);
        FormResponse saved = saveFormResponsePort.save(response);

        List<AnswerWithOptions> data = buildAnswerData(saved, answersOnPath);
        saveAnswers(data);

        return saved.getId();
    }

    @Override
    public void updateResponse(UpdateFormResponseCommand command) {
        requireRespondentMemberId(command.respondentMemberId());
        Form form = loadPublishedForm(command.formId());
        validateSingleResponseLookupPolicy(form);

        FormResponse existing = loadFormResponsePort
            .findSubmittedByFormIdAndRespondentMemberId(command.formId(), command.respondentMemberId())
            .orElseThrow(() -> new FormDomainException(FormErrorCode.FORM_RESPONSE_NOT_FOUND));

        validateSubmitScope(command.formId(), command.allowedQuestionIds(), command.requiredQuestionIds());
        validateAnswers(command.formId(), command.answers());
        Set<Long> answeredQuestionIds = extractQuestionIds(command.answers());
        if (command.allowedQuestionIds() != null) {
            validateAnsweredQuestionsAllowed(command.allowedQuestionIds(), answeredQuestionIds);
        }
        validateAllRequiredAnsweredOnPath(
            command.formId(),
            answeredQuestionIds,
            extractSingleSelectedOptionIds(command.answers()),
            command.requiredQuestionIds()
        );

        saveAnswerPort.deleteAllByFormResponseId(existing.getId());

        List<AnswerWithOptions> data = buildAnswerData(existing, command.answers());
        saveAnswers(data);
        saveEmptyAnswersForUnanswered(existing, command.allowedQuestionIds(), answeredQuestionIds);

        existing.updateLastSavedAt(Instant.now());
        saveFormResponsePort.save(existing);
    }

    @Override
    public void deleteResponse(DeleteFormResponseCommand command) {
        requireRespondentMemberId(command.respondentMemberId());
        Form form = loadPublishedForm(command.formId());
        validateSingleResponseLookupPolicy(form);

        FormResponse existing = loadFormResponsePort
            .findSubmittedByFormIdAndRespondentMemberId(command.formId(), command.respondentMemberId())
            .orElseThrow(() -> new FormDomainException(FormErrorCode.FORM_RESPONSE_NOT_FOUND));

        saveAnswerPort.deleteAllByFormResponseId(existing.getId());
        saveFormResponsePort.deleteById(existing.getId());
    }

    @Override
    public Long createDraft(CreateDraftFormResponseCommand command) {
        requireRespondentMemberId(command.respondentMemberId());
        Form form = loadPublishedForm(command.formId());

        validateDuplicateResponsePolicy(form, command.respondentMemberId());

        FormResponse draft = FormResponse.createDraft(form, command.respondentMemberId());
        return saveFormResponsePort.save(draft).getId();
    }

    @Override
    public void updateDraft(UpdateDraftFormResponseCommand command) {
        FormResponse draft = loadDraftAsOwner(command.formResponseId(), command.requesterMemberId());

        // 형식 검증만 수행 — 작성 중이라 필수 누락은 정상
        validateAnswers(draft.getForm().getId(), command.answers());

        // 기존 답변 전체 교체
        saveAnswerPort.deleteAllByFormResponseId(draft.getId());
        List<AnswerWithOptions> data = buildAnswerData(draft, command.answers());
        saveAnswers(data);

        draft.updateLastSavedAt(Instant.now());
        saveFormResponsePort.save(draft);
    }

    @Override
    public void submitDraft(SubmitDraftFormResponseCommand command) {
        FormResponse draft = loadDraftAsOwner(command.formResponseId(), command.requesterMemberId());
        validateSubmitScope(draft.getForm().getId(), command.allowedQuestionIds(), command.requiredQuestionIds());

        List<Answer> savedAnswers = loadAnswerPort.listByFormResponseId(draft.getId());
        // draft 저장 이후 폼 스키마가 바뀌었는지 재검증한다.
        // - 참조 Question 이 하드 삭제된 answer 는 조용히 정리 후 나머지만 검증
        // - 남은 answer 가 현재 스키마 (type / 옵션 / 파일) 와 어긋나면 DRAFT_SCHEMA_MISMATCH 로 거부
        savedAnswers = revalidateDraftAnswersAgainstSchema(draft, savedAnswers);
        Set<Long> answeredQuestionIds = savedAnswers.stream()
            .map(answer -> answer.getQuestion().getId())
            .collect(Collectors.toSet());

        if (command.allowedQuestionIds() != null) {
            validateAnsweredQuestionsAllowed(command.allowedQuestionIds(), answeredQuestionIds);
        }

        Map<Long, Long> selectedOptionByQuestion = loadSelectedOptionByQuestion(savedAnswers);
        Set<Long> visitedSectionIds = resolveVisitedSectionIds(draft.getForm().getId(), selectedOptionByQuestion);
        validateAllRequiredAnsweredWithVisited(
            draft.getForm().getId(),
            answeredQuestionIds,
            visitedSectionIds,
            command.requiredQuestionIds()
        );

        // : 방문 경로 밖 섹션에 draft 로 저장된 답변은 SUBMITTED 결과에 포함하지 않도록 조용히 폐기한다.
        discardOrphanAnswersOnSubmit(draft.getId(), draft.getForm().getId(), visitedSectionIds);
        Set<Long> answeredQuestionIdsOnPath = filterOnVisitedPath(
            draft.getForm().getId(),
            answeredQuestionIds,
            visitedSectionIds
        );

        saveEmptyAnswersForUnanswered(
            draft,
            filterOnVisitedPath(draft.getForm().getId(), command.allowedQuestionIds(), visitedSectionIds),
            answeredQuestionIdsOnPath
        );

        draft.submit(Instant.now(), command.submittedIp());
        saveFormResponsePort.save(draft);
    }

    @Override
    public void deleteDraft(DeleteDraftFormResponseCommand command) {
        FormResponse draft = loadDraftAsOwner(command.formResponseId(), command.requesterMemberId());

        saveAnswerPort.deleteAllByFormResponseId(draft.getId());
        saveFormResponsePort.deleteById(draft.getId());
    }

    @Override
    public AnonymousFormResponseResult submitAnonymousImmediately(SubmitAnonymousImmediatelyFormResponseCommand command) {
        Form form = loadPublishedForm(command.formId());

        // 익명은 중복 정책 검사 skip — 소비 도메인(리크루팅 등) 이 자체 rate limit / 유일성 검사로 방어.
        validateAnswers(command.formId(), command.answers());
        Set<Long> visitedSectionIds = resolveVisitedSectionIds(
            command.formId(),
            extractSingleSelectedOptionIds(command.answers())
        );
        validateAllRequiredAnsweredWithVisited(
            command.formId(),
            extractQuestionIds(command.answers()),
            visitedSectionIds,
            null
        );

        // : 방문 경로 밖 섹션의 답변은 SUBMITTED 결과에 포함하지 않도록 조용히 폐기한다.
        List<AnswerCommand> answersOnPath = filterAnswersOnVisitedPath(
            command.formId(),
            command.answers(),
            visitedSectionIds
        );

        String rawAccessKey = secureTokenGenerator.generateOpaqueToken();
        String accessKeyHash = secureTokenGenerator.sha256Hex(rawAccessKey);

        FormResponse response = FormResponse.createAnonymousDraft(form, accessKeyHash);
        response.submit(Instant.now(), null);
        FormResponse saved = saveFormResponsePort.save(response);

        List<AnswerWithOptions> data = buildAnswerData(saved, answersOnPath);
        saveAnswers(data);

        return AnonymousFormResponseResult.builder()
            .formResponseId(saved.getId())
            .responseAccessKey(rawAccessKey)
            .build();
    }

    @Override
    public void updateAnonymousResponse(UpdateAnonymousFormResponseCommand command) {
        FormResponse existing = loadSubmittedAsAnonymous(command.responseAccessKey());

        validateSubmitScope(
            existing.getForm().getId(),
            command.allowedQuestionIds(),
            command.requiredQuestionIds()
        );
        validateAnswers(existing.getForm().getId(), command.answers());
        Set<Long> answeredQuestionIds = extractQuestionIds(command.answers());
        if (command.allowedQuestionIds() != null) {
            validateAnsweredQuestionsAllowed(command.allowedQuestionIds(), answeredQuestionIds);
        }
        validateAllRequiredAnsweredOnPath(
            existing.getForm().getId(),
            answeredQuestionIds,
            extractSingleSelectedOptionIds(command.answers()),
            command.requiredQuestionIds()
        );

        saveAnswerPort.deleteAllByFormResponseId(existing.getId());

        List<AnswerWithOptions> data = buildAnswerData(existing, command.answers());
        saveAnswers(data);
        saveEmptyAnswersForUnanswered(existing, command.allowedQuestionIds(), answeredQuestionIds);

        existing.updateLastSavedAt(Instant.now());
        saveFormResponsePort.save(existing);
    }

    @Override
    public void deleteAnonymousResponse(DeleteAnonymousFormResponseCommand command) {
        FormResponse existing = loadSubmittedAsAnonymous(command.responseAccessKey());

        saveAnswerPort.deleteAllByFormResponseId(existing.getId());
        saveFormResponsePort.deleteById(existing.getId());
    }

    @Override
    public AnonymousFormResponseResult createAnonymousDraft(CreateAnonymousDraftFormResponseCommand command) {
        Form form = loadPublishedForm(command.formId());

        // 익명은 중복 정책 검사 skip — 소비 도메인(리크루팅 등) 이 자체 rate limit / 유일성 검사로 방어.
        String rawAccessKey = secureTokenGenerator.generateOpaqueToken();
        String accessKeyHash = secureTokenGenerator.sha256Hex(rawAccessKey);

        FormResponse draft = FormResponse.createAnonymousDraft(form, accessKeyHash);
        FormResponse saved = saveFormResponsePort.save(draft);

        return AnonymousFormResponseResult.builder()
            .formResponseId(saved.getId())
            .responseAccessKey(rawAccessKey)
            .build();
    }

    @Override
    public void updateAnonymousDraft(UpdateAnonymousDraftFormResponseCommand command) {
        FormResponse draft = loadDraftAsAnonymous(command.responseAccessKey());

        // 형식 검증만 수행 — 작성 중이라 필수 누락은 정상
        validateAnswers(draft.getForm().getId(), command.answers());

        // 기존 답변 전체 교체
        saveAnswerPort.deleteAllByFormResponseId(draft.getId());
        List<AnswerWithOptions> data = buildAnswerData(draft, command.answers());
        saveAnswers(data);

        draft.updateLastSavedAt(Instant.now());
        saveFormResponsePort.save(draft);
    }

    @Override
    public void submitAnonymousDraft(SubmitAnonymousDraftFormResponseCommand command) {
        FormResponse draft = loadDraftAsAnonymous(command.responseAccessKey());
        validateSubmitScope(draft.getForm().getId(), command.allowedQuestionIds(), command.requiredQuestionIds());

        List<Answer> savedAnswers = loadAnswerPort.listByFormResponseId(draft.getId());
        savedAnswers = revalidateDraftAnswersAgainstSchema(draft, savedAnswers);
        Set<Long> answeredQuestionIds = savedAnswers.stream()
            .map(answer -> answer.getQuestion().getId())
            .collect(Collectors.toSet());

        if (command.allowedQuestionIds() != null) {
            validateAnsweredQuestionsAllowed(command.allowedQuestionIds(), answeredQuestionIds);
        }

        Map<Long, Long> selectedOptionByQuestion = loadSelectedOptionByQuestion(savedAnswers);
        Set<Long> visitedSectionIds = resolveVisitedSectionIds(draft.getForm().getId(), selectedOptionByQuestion);
        validateAllRequiredAnsweredWithVisited(
            draft.getForm().getId(),
            answeredQuestionIds,
            visitedSectionIds,
            command.requiredQuestionIds()
        );

        // : 방문 경로 밖 섹션에 draft 로 저장된 답변은 SUBMITTED 결과에 포함하지 않도록 조용히 폐기한다.
        discardOrphanAnswersOnSubmit(draft.getId(), draft.getForm().getId(), visitedSectionIds);
        Set<Long> answeredQuestionIdsOnPath = filterOnVisitedPath(
            draft.getForm().getId(),
            answeredQuestionIds,
            visitedSectionIds
        );

        saveEmptyAnswersForUnanswered(
            draft,
            filterOnVisitedPath(draft.getForm().getId(), command.allowedQuestionIds(), visitedSectionIds),
            answeredQuestionIdsOnPath
        );

        draft.submit(Instant.now(), command.submittedIp());
        saveFormResponsePort.save(draft);
    }

    @Override
    public void deleteAnonymousDraft(DeleteAnonymousDraftFormResponseCommand command) {
        FormResponse draft = loadDraftAsAnonymous(command.responseAccessKey());

        saveAnswerPort.deleteAllByFormResponseId(draft.getId());
        saveFormResponsePort.deleteById(draft.getId());
    }

    @Override
    public Long claimAnonymousResponse(ClaimAnonymousFormResponseCommand command) {
        requireRespondentMemberId(command.requesterMemberId());
        if (command.responseAccessKey() == null) {
            throw new FormDomainException(FormErrorCode.RESPONSE_ACCESS_KEY_REQUIRED);
        }

        FormResponse response = loadFormResponsePort.findById(command.formResponseId())
            .orElseThrow(() -> new FormDomainException(FormErrorCode.FORM_RESPONSE_NOT_FOUND));

        if (response.getRespondentMemberId() != null) {
            throw new FormDomainException(FormErrorCode.FORM_RESPONSE_ALREADY_CLAIMED);
        }

        String hash = secureTokenGenerator.sha256Hex(command.responseAccessKey());
        if (!hash.equals(response.getResponseAccessKeyHash())) {
            throw new FormDomainException(FormErrorCode.FORM_RESPONSE_FORBIDDEN);
        }

        validateDuplicateResponsePolicy(response.getForm(), command.requesterMemberId());

        response.claimBy(command.requesterMemberId());
        FormResponse saved = saveFormResponsePort.save(response);

        FormResponseStatus previousState = saved.getStatus();
        log.info(
            "Anonymous form response claimed by member: formResponse={}, member={}, previousState={}",
            saved.getId(),
            command.requesterMemberId(),
            previousState
        );

        return saved.getId();
    }

    /**
     * 응답 ID 로 DRAFT 응답 로드. 없으면 NOT_FOUND, DRAFT 가 아니면 NOT_DRAFT 예외.
     */
    private FormResponse loadDraft(Long formResponseId) {
        FormResponse formResponse = loadFormResponsePort.findById(formResponseId)
            .orElseThrow(() -> new FormDomainException(FormErrorCode.FORM_RESPONSE_NOT_FOUND));
        requirePublished(formResponse.getForm());
        if (formResponse.getStatus() != FormResponseStatus.DRAFT) {
            throw new FormDomainException(FormErrorCode.FORM_RESPONSE_NOT_DRAFT);
        }
        return formResponse;
    }

    /**
     * 익명 draft 응답 로드 + 검증 (익명 전용).
     * <p>
     * 순서: rawKey null 방어 → sha256 계산 → hash 매칭으로 DRAFT 조회 → 익명 여부 확인.
     * <p>
     * 다음 경우 모두 FORBIDDEN 처리:
     * <ul>
     *   <li>hash 매칭 실패 (잘못된 key 또는 이미 SUBMITTED 로 전이됨)</li>
     *   <li>기명 draft ({@code respondentMemberId != null}) — 익명 UseCase 로 접근 불가</li>
     * </ul>
     * rawKey 가 null 이면 {@link FormErrorCode#RESPONSE_ACCESS_KEY_REQUIRED}.
     */
    private FormResponse loadDraftAsAnonymous(String rawAccessKey) {
        if (rawAccessKey == null) {
            throw new FormDomainException(FormErrorCode.RESPONSE_ACCESS_KEY_REQUIRED);
        }
        String hash = secureTokenGenerator.sha256Hex(rawAccessKey);
        FormResponse draft = loadFormResponsePort.findDraftByAccessKeyHash(hash)
            .orElseThrow(() -> new FormDomainException(FormErrorCode.FORM_RESPONSE_FORBIDDEN));
        requirePublished(draft.getForm());
        if (draft.getRespondentMemberId() != null) {
            throw new FormDomainException(FormErrorCode.FORM_RESPONSE_FORBIDDEN);
        }
        return draft;
    }

    /**
     * 익명 SUBMITTED 응답 로드 + 검증 (익명 전용).
     * <p>
     * 순서: rawKey null 방어 → sha256 계산 → hash 매칭으로 SUBMITTED 조회 → 익명 여부 확인.
     * <p>
     * 다음 경우 모두 FORBIDDEN 처리:
     * <ul>
     *   <li>hash 매칭 실패 (잘못된 key 또는 아직 DRAFT 상태)</li>
     *   <li>기명 응답 ({@code respondentMemberId != null}) — 익명 UseCase 로 접근 불가</li>
     * </ul>
     * rawKey 가 null 이면 {@link FormErrorCode#RESPONSE_ACCESS_KEY_REQUIRED}.
     */
    private FormResponse loadSubmittedAsAnonymous(String rawAccessKey) {
        if (rawAccessKey == null) {
            throw new FormDomainException(FormErrorCode.RESPONSE_ACCESS_KEY_REQUIRED);
        }
        String hash = secureTokenGenerator.sha256Hex(rawAccessKey);
        FormResponse response = loadFormResponsePort.findSubmittedByAccessKeyHash(hash)
            .orElseThrow(() -> new FormDomainException(FormErrorCode.FORM_RESPONSE_FORBIDDEN));
        requirePublished(response.getForm());
        if (response.getRespondentMemberId() != null) {
            throw new FormDomainException(FormErrorCode.FORM_RESPONSE_FORBIDDEN);
        }
        return response;
    }

    /**
     * 소유자 검증까지 포함한 DRAFT 응답 로드 (기명 전용).
     * <p>
     * 순서: DRAFT 로드({@link #loadDraft}) → 소유자 대조.
     * 소유자와 일치하지 않으면 {@link FormErrorCode#FORM_RESPONSE_FORBIDDEN} 예외.
     * <p>
     * 다음 경우 모두 FORBIDDEN 처리:
     * <ul>
     *   <li>익명 draft({@code respondentMemberId=null}) — 기명 UseCase 로 접근 불가, 별도 익명 UseCase(추후 도입) 사용</li>
     *   <li>요청자 memberId 가 draft 소유자와 다름</li>
     *   <li>요청자 memberId 가 null (auth 계층에서 걸러졌어야 하는 케이스, 방어 목적)</li>
     * </ul>
     */
    private FormResponse loadDraftAsOwner(Long formResponseId, Long requesterMemberId) {
        FormResponse draft = loadDraft(formResponseId);
        if (draft.getRespondentMemberId() == null
            || !draft.getRespondentMemberId().equals(requesterMemberId)) {
            throw new FormDomainException(FormErrorCode.FORM_RESPONSE_FORBIDDEN);
        }
        return draft;
    }

    private static Set<Long> extractQuestionIds(List<AnswerCommand> answers) {
        return answers.stream()
            .map(AnswerCommand::questionId)
            .collect(Collectors.toSet());
    }

    private Form loadPublishedForm(Long formId) {
        Form form = loadFormPort.findById(formId)
            .orElseThrow(() -> new FormDomainException(FormErrorCode.FORM_NOT_FOUND));
        requirePublished(form);
        return form;
    }

    private static void requirePublished(Form form) {
        if (!form.isPublished()) {
            throw new FormDomainException(FormErrorCode.FORM_NOT_PUBLISHED);
        }
    }

    private void validateDuplicateResponsePolicy(Form form, Long respondentMemberId) {
        if (form.isAllowDuplicateResponses()) {
            return;
        }
        if (respondentMemberId == null) {
            return;
        }
        if (loadFormResponsePort.existsByFormIdAndMemberId(form.getId(), respondentMemberId)) {
            throw new FormDomainException(FormErrorCode.FORM_RESPONSE_ALREADY_EXISTS);
        }
    }

    private static void validateSingleResponseLookupPolicy(Form form) {
        if (form.isAllowDuplicateResponses()) {
            throw new FormDomainException(FormErrorCode.FORM_RESPONSE_LOOKUP_AMBIGUOUS);
        }
    }

    private static void requireRespondentMemberId(Long respondentMemberId) {
        if (respondentMemberId == null) {
            throw new FormDomainException(FormErrorCode.RESPONDENT_MEMBER_ID_REQUIRED);
        }
    }

    /**
     * 답변 형식 / 질문 소속 / 옵션 소속 등 형식 검증만 수행. 필수 답변 누락 검증은 별도.
     * <p>
     * draft 작성 중 (updateDraft) 에는 필수 누락이 정상이라 형식만 검증.
     * 제출 시점 (submitImmediately, updateResponse, submitDraft) 에는 별도로 {@link #validateAllRequiredAnsweredOnPath} 호출 필요.
     */
    private void validateAnswers(Long formId, List<AnswerCommand> answers) {
        if (answers == null) {
            throw new FormDomainException(FormErrorCode.INVALID_ANSWER_FORMAT);
        }

        List<Question> formQuestions = loadQuestionPort.listByFormId(formId);

        Set<Long> answeredQuestionIds = new HashSet<>();
        for (AnswerCommand answerCommand : answers) {
            if (answerCommand.questionId() == null) {
                throw new FormDomainException(FormErrorCode.INVALID_ANSWER_FORMAT);
            }
            if (!answeredQuestionIds.add(answerCommand.questionId())) {
                throw new FormDomainException(FormErrorCode.INVALID_ANSWER_FORMAT);
            }
            Question question = formQuestions.stream()
                .filter(q -> q.getId().equals(answerCommand.questionId()))
                .findFirst()
                .orElseThrow(() -> new FormDomainException(FormErrorCode.QUESTION_IS_NOT_OWNED_BY_FORM));

            validateAnswerAgainstQuestion(answerCommand, question);
        }
    }

    /**
     * 응답자가 실제 방문한 섹션 경로 상의 필수 질문만 검증. 제출 시점에만 호출.
     * 조건부 섹션 이동이 없는 폼은 전체 섹션을 방문하므로 기존 동작과 동일.
     */
    private void validateAllRequiredAnsweredOnPath(
        Long formId,
        Set<Long> answeredQuestionIds,
        Map<Long, Long> selectedOptionByQuestion
    ) {
        Set<Long> visitedSectionIds = resolveVisitedSectionIds(formId, selectedOptionByQuestion);
        validateAllRequiredAnsweredWithVisited(formId, answeredQuestionIds, visitedSectionIds, null);
    }

    /**
     * 방문 경로와 caller 제공 required 를 통합해 검증한다.
     * <p>
     * {@code callerRequiredQuestionIds} 가 {@code null} 이면 폼 질문의 {@code isRequired} 를 사용하고,
     * 제공되면 방문 경로 질문과의 교집합만 필수로 취급한다. 조건부 섹션 이동으로 건너뛴 질문은 caller 가 required 로 전달했더라도 검증에서 제외된다.
     */
    private void validateAllRequiredAnsweredWithVisited(
        Long formId,
        Set<Long> answeredQuestionIds,
        Set<Long> visitedSectionIds,
        Set<Long> callerRequiredQuestionIds
    ) {
        List<Question> formQuestions = loadQuestionPort.listByFormId(formId);
        for (Question q : formQuestions) {
            if (!visitedSectionIds.contains(q.getFormSection().getId())) {
                continue;
            }
            boolean required = callerRequiredQuestionIds != null
                ? callerRequiredQuestionIds.contains(q.getId())
                : Boolean.TRUE.equals(q.getIsRequired());
            if (required && !answeredQuestionIds.contains(q.getId())) {
                throw new FormDomainException(FormErrorCode.REQUIRED_QUESTION_NOT_ANSWERED);
            }
        }
    }

    /**
     * savedAnswers 로부터 (questionId → selectedOptionId) 맵을 계산한다.
     * <p>
     * 방문 경로 계산용. 이미 메모리에 있는 savedAnswers 로 answerId → questionId 매핑을 미리 만들어
     * AnswerChoice 순회 시 Answer 프록시 초기화로 인한 N+1 을 회피한다.
     * RADIO/DROPDOWN 이 아닌 답변의 선택지는 방문 경로 계산에 사용되지 않지만,
     * 이 헬퍼는 관심 없이 모든 선택지를 담아 리턴한다. 소비 측에서 필터링한다.
     */
    private Map<Long, Long> loadSelectedOptionByQuestion(List<Answer> savedAnswers) {
        if (savedAnswers.isEmpty()) {
            return Map.of();
        }
        Map<Long, Long> answerIdToQuestionId = savedAnswers.stream()
            .collect(Collectors.toMap(Answer::getId, a -> a.getQuestion().getId()));
        return loadAnswerPort.listChoicesByAnswerIdIn(answerIdToQuestionId.keySet()).stream()
            .filter(c -> c.getQuestionOption() != null)
            .collect(Collectors.toMap(
                c -> answerIdToQuestionId.get(c.getAnswer().getId()),
                c -> c.getQuestionOption().getId(),
                (a, b) -> a
            ));
    }

    /**
     * 폼 질문 중 방문 경로 밖 섹션에 속한 질문 ID 집합을 반환한다 ( orphan 정리 용).
     */
    private Set<Long> resolveOrphanQuestionIds(Long formId, Set<Long> visitedSectionIds) {
        return loadQuestionPort.listByFormId(formId).stream()
            .filter(q -> !visitedSectionIds.contains(q.getFormSection().getId()))
            .map(Question::getId)
            .collect(Collectors.toSet());
    }

    /**
     * 요청 payload 의 answer 목록에서 방문 경로 밖 섹션의 답변을 제거한다 ( 즉시 제출 경로 용).
     * <p>
     * 폐기 건이 있으면 관측용 INFO 로그를 남긴다. (form_response 는 아직 미저장 단계라 formResponseId 는 로그에서 생략.)
     */
    private List<AnswerCommand> filterAnswersOnVisitedPath(
        Long formId,
        List<AnswerCommand> answers,
        Set<Long> visitedSectionIds
    ) {
        Set<Long> orphanQuestionIds = resolveOrphanQuestionIds(formId, visitedSectionIds);
        if (orphanQuestionIds.isEmpty()) {
            return answers;
        }
        List<AnswerCommand> onPath = answers.stream()
            .filter(a -> !orphanQuestionIds.contains(a.questionId()))
            .toList();
        int discarded = answers.size() - onPath.size();
        if (discarded > 0) {
            log.info(
                "Discarded {} orphan answers on submit for form_id={} (pre-persist immediate submit)",
                discarded,
                formId
            );
        }
        return onPath;
    }

    /**
     * 방문 경로 밖 섹션에 속한 question ID 를 제거한 집합을 반환한다.
     * <p>
     * questionIds 가 null 이면 그대로 반환 (allowedQuestionIds 비프로젝트 경로 대응).
     */
    private Set<Long> filterOnVisitedPath(Long formId, Set<Long> questionIds, Set<Long> visitedSectionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            return questionIds;
        }
        Set<Long> orphanQuestionIds = resolveOrphanQuestionIds(formId, visitedSectionIds);
        if (orphanQuestionIds.isEmpty()) {
            return questionIds;
        }
        return questionIds.stream()
            .filter(id -> !orphanQuestionIds.contains(id))
            .collect(Collectors.toSet());
    }

    /**
     * draft 제출 시 저장돼 있던 answer 를 현재 스키마와 대조한다.
     * <p>
     * 순서:
     * <ol>
     *   <li>참조 Question 이 하드 삭제된 answer 는 조용히 삭제 (INFO 로그) 후 나머지 리스트로 이어간다.</li>
     *   <li>남은 answer 를 현재 Question 의 type / 옵션 / 파일과 대조한다.
     *       어긋난 answer 의 questionId 를 모아 {@link DraftSchemaMismatchException} 으로 400 응답.</li>
     * </ol>
     * <p>
     * 참조 Question 이 {@code isActive=false} (#822 fork 로 새 활성 버전이 생긴 상태) 이면 stale 로 판정한다.
     * fork 는 이미 SUBMITTED 된 응답의 스키마 보존이 목적이고, draft 는 아직 확정 전이라 fork 발생 시 새 활성 스키마 기준 재확인이 더 안전하다.
     * <p>
     * 반환값: hard-deleted 참조를 제외한 살아남은 answer 목록. 이후 필수 검증, 방문 경로 계산, orphan 정리에 사용.
     */
    private List<Answer> revalidateDraftAnswersAgainstSchema(FormResponse draft, List<Answer> savedAnswers) {
        if (savedAnswers.isEmpty()) {
            return savedAnswers;
        }

        Long formResponseId = draft.getId();

        Set<Long> referencedQuestionIds = savedAnswers.stream()
            .map(a -> a.getQuestion().getId())
            .collect(Collectors.toSet());
        List<Question> existingQuestions = loadQuestionPort.listByIdIn(referencedQuestionIds);
        Set<Long> existingQuestionIds = existingQuestions.stream()
            .map(Question::getId)
            .collect(Collectors.toSet());

        List<Answer> survivingAnswers;
        Set<Long> hardDeletedIds = referencedQuestionIds.stream()
            .filter(id -> !existingQuestionIds.contains(id))
            .collect(Collectors.toSet());
        if (!hardDeletedIds.isEmpty()) {
            int discarded = saveAnswerPort.deleteByFormResponseIdAndQuestionIdIn(formResponseId, hardDeletedIds);
            if (discarded > 0) {
                log.info(
                    "Discarded {} answers referencing hard-deleted questions on draft submit for form_response={}",
                    discarded,
                    formResponseId
                );
            }
            survivingAnswers = savedAnswers.stream()
                .filter(a -> existingQuestionIds.contains(a.getQuestion().getId()))
                .toList();
        } else {
            survivingAnswers = savedAnswers;
        }

        if (survivingAnswers.isEmpty()) {
            return survivingAnswers;
        }

        Map<Long, Question> questionById = existingQuestions.stream()
            .collect(Collectors.toMap(Question::getId, Function.identity()));

        Set<Long> choiceQuestionIds = survivingAnswers.stream()
            .filter(a -> isChoiceType(a.getAnsweredAsType()))
            .map(a -> a.getQuestion().getId())
            .collect(Collectors.toSet());
        Map<Long, Set<Long>> validOptionIdsByQuestion = new HashMap<>();
        if (!choiceQuestionIds.isEmpty()) {
            loadQuestionOptionPort.listByQuestionIdIn(choiceQuestionIds).forEach(opt -> {
                Long qid = opt.getQuestion().getId();
                validOptionIdsByQuestion.computeIfAbsent(qid, k -> new HashSet<>()).add(opt.getId());
            });
        }

        Set<Long> choiceAnswerIds = survivingAnswers.stream()
            .filter(a -> isChoiceType(a.getAnsweredAsType()))
            .map(Answer::getId)
            .collect(Collectors.toSet());
        Map<Long, List<AnswerChoice>> choicesByAnswerId = new HashMap<>();
        if (!choiceAnswerIds.isEmpty()) {
            loadAnswerPort.listChoicesByAnswerIdIn(choiceAnswerIds).forEach(choice -> {
                Long aid = choice.getAnswer().getId();
                choicesByAnswerId.computeIfAbsent(aid, k -> new ArrayList<>()).add(choice);
            });
        }

        List<Long> staleQuestionIds = new ArrayList<>();
        for (Answer answer : survivingAnswers) {
            Question current = questionById.get(answer.getQuestion().getId());
            if (!isAnswerCompatibleWithCurrentSchema(answer, current, validOptionIdsByQuestion, choicesByAnswerId)) {
                staleQuestionIds.add(current.getId());
            }
        }

        if (!staleQuestionIds.isEmpty()) {
            throw new DraftSchemaMismatchException(staleQuestionIds);
        }

        return survivingAnswers;
    }

    private static boolean isChoiceType(QuestionType type) {
        return type == QuestionType.RADIO
            || type == QuestionType.DROPDOWN
            || type == QuestionType.CHECKBOX;
    }

    private boolean isAnswerCompatibleWithCurrentSchema(
        Answer answer,
        Question current,
        Map<Long, Set<Long>> validOptionIdsByQuestion,
        Map<Long, List<AnswerChoice>> choicesByAnswerId
    ) {
        // 참조 Question 이 isActive=false 면 #822 fork 로 교체된 상태 — draft 는 새 활성 스키마 기준
        // 으로 재확인이 필요하므로 stale 로 판정.
        if (!current.isActive()) {
            return false;
        }
        QuestionType answeredAsType = answer.getAnsweredAsType();
        QuestionType currentType = current.getType();

        if (isChoiceType(answeredAsType)) {
            if (!isChoiceType(currentType)) {
                return false;
            }
            Set<Long> validOptions = validOptionIdsByQuestion.getOrDefault(current.getId(), Set.of());
            List<AnswerChoice> choices = choicesByAnswerId.getOrDefault(answer.getId(), List.of());
            if (choices.isEmpty()) {
                return false;
            }
            for (AnswerChoice choice : choices) {
                QuestionOption opt = choice.getQuestionOption();
                if (opt == null || !validOptions.contains(opt.getId())) {
                    return false;
                }
            }
            return true;
        }

        if (answeredAsType == QuestionType.FILE) {
            if (currentType != QuestionType.FILE) {
                return false;
            }
            Set<String> fileIds = answer.getFileIds();
            if (fileIds != null) {
                for (String fileId : fileIds) {
                    if (!getFileUseCase.existsById(fileId)) {
                        return false;
                    }
                }
            }
            return true;
        }

        // SHORT_TEXT / LONG_TEXT / SCHEDULE / PORTFOLIO — type equality check
        return currentType == answeredAsType;
    }

    /**
     * 제출 시점에 방문 경로 밖 섹션의 answer 를 조용히 삭제한다 (정책: silent auto-clean).
     * <p>
     * 조건부 섹션 이동으로 건너뛴 섹션에 draft 로 저장돼 있던 답변이 SUBMITTED 결과에 포함되지 않도록 정리한다.
     * SUBMITTED 상태 전이와 동일 트랜잭션에서 수행돼야 하므로 {@code @Transactional} 서비스 메서드 내부에서만 호출한다.
     * 사용자에게 별도 에러를 반환하지 않으며, 관측용으로 INFO 로그만 남긴다.
     */
    private void discardOrphanAnswersOnSubmit(Long formResponseId, Long formId, Set<Long> visitedSectionIds) {
        Set<Long> orphanQuestionIds = resolveOrphanQuestionIds(formId, visitedSectionIds);
        if (orphanQuestionIds.isEmpty()) {
            return;
        }
        int discarded = saveAnswerPort.deleteByFormResponseIdAndQuestionIdIn(formResponseId, orphanQuestionIds);
        if (discarded > 0) {
            log.info(
                "Discarded {} orphan answers on submit for form_response={}",
                discarded,
                formResponseId
            );
        }
    }

    /**
     * 제출된 답변의 선택지를 기반으로 응답자가 실제로 방문한 섹션 ID 집합을 계산한다.
     * RADIO/DROPDOWN 선택지에 nextSectionId가 지정된 경우 해당 섹션으로 점프하고,
     * 없으면 orderNo 오름차순으로 다음 섹션으로 이동한다.
     */
    private Set<Long> resolveVisitedSectionIds(Long formId, Map<Long, Long> selectedOptionByQuestion) {
        List<FormSection> sections = loadFormSectionPort.listByFormId(formId).stream()
            .sorted(Comparator.comparing(FormSection::getOrderNo))
            .toList();
        if (sections.isEmpty()) {
            return Set.of();
        }

        List<Question> questions = loadQuestionPort.listByFormId(formId);

        Set<Long> radioDropdownQuestionIds = questions.stream()
            .filter(q -> q.getType() == QuestionType.RADIO || q.getType() == QuestionType.DROPDOWN)
            .map(Question::getId)
            .collect(Collectors.toSet());

        Map<Long, Long> optionToNextSection = new HashMap<>();
        if (!radioDropdownQuestionIds.isEmpty()) {
            loadQuestionOptionPort.listByQuestionIdIn(radioDropdownQuestionIds).stream()
                .filter(opt -> opt.getNextSectionId() != null)
                .forEach(opt -> optionToNextSection.put(opt.getId(), opt.getNextSectionId()));
        }

        Map<Long, List<Question>> questionsBySection = questions.stream()
            .collect(Collectors.groupingBy(q -> q.getFormSection().getId()));
        Map<Long, FormSection> sectionById = sections.stream()
            .collect(Collectors.toMap(FormSection::getId, Function.identity()));

        Set<Long> visited = new LinkedHashSet<>();
        FormSection current = sections.get(0);

        while (current != null) {
            visited.add(current.getId());

            FormSection next = null;
            for (Question q : questionsBySection.getOrDefault(current.getId(), List.of())) {
                if (q.getType() != QuestionType.RADIO && q.getType() != QuestionType.DROPDOWN) continue;
                Long selectedOptionId = selectedOptionByQuestion.get(q.getId());
                if (selectedOptionId == null) continue;
                Long nextSectionId = optionToNextSection.get(selectedOptionId);
                if (nextSectionId != null && !visited.contains(nextSectionId)) {
                    next = sectionById.get(nextSectionId);
                    break;
                }
            }

            if (next == null) {
                int currentIndex = sections.indexOf(current);
                next = (currentIndex != -1 && currentIndex < sections.size() - 1)
                    ? sections.get(currentIndex + 1)
                    : null;
            }

            current = next;
        }

        return visited;
    }

    private static Map<Long, Long> extractSingleSelectedOptionIds(List<AnswerCommand> answers) {
        return answers.stream()
            .filter(a -> a.selectedOptionIds() != null && a.selectedOptionIds().size() == 1)
            .collect(Collectors.toMap(
                AnswerCommand::questionId,
                a -> a.selectedOptionIds().get(0)
            ));
    }

    private void validateAnsweredQuestionsAllowed(Set<Long> allowedQuestionIds, Set<Long> answeredQuestionIds) {
        for (Long questionId : answeredQuestionIds) {
            if (!allowedQuestionIds.contains(questionId)) {
                throw new FormDomainException(FormErrorCode.QUESTION_IS_NOT_OWNED_BY_FORM);
            }
        }
    }

    /**
     * 제출 scope 검증 — allowedQuestionIds / requiredQuestionIds 가 현재 폼 소속이고 required ⊆ allowed 인지 확인.
     * <p>
     * 미검증 시 폼 A draft 에 폼 B 질문 ID 를 넘겨 교차 폼 Answer 저장 등의 무결성 파괴가 가능하다.
     */
    private void validateSubmitScope(Long formId, Set<Long> allowedQuestionIds, Set<Long> requiredQuestionIds) {
        if (allowedQuestionIds == null && requiredQuestionIds == null) {
            return;
        }
        Set<Long> formQuestionIds = loadQuestionPort.listByFormId(formId).stream()
            .map(Question::getId)
            .collect(Collectors.toSet());
        if (allowedQuestionIds != null && !formQuestionIds.containsAll(allowedQuestionIds)) {
            throw new FormDomainException(FormErrorCode.QUESTION_IS_NOT_OWNED_BY_FORM);
        }
        if (requiredQuestionIds != null && !formQuestionIds.containsAll(requiredQuestionIds)) {
            throw new FormDomainException(FormErrorCode.QUESTION_IS_NOT_OWNED_BY_FORM);
        }
        if (allowedQuestionIds != null && requiredQuestionIds != null
            && !allowedQuestionIds.containsAll(requiredQuestionIds)) {
            throw new FormDomainException(FormErrorCode.INVALID_SUBMIT_SCOPE);
        }
    }

    private void validateAnswerAgainstQuestion(AnswerCommand answerCommand, Question question) {
        QuestionType type = question.getType();
        switch (type) {
            case SHORT_TEXT, LONG_TEXT -> {
                if (answerCommand.textValue() == null || answerCommand.textValue().isBlank()) {
                    throw new FormDomainException(FormErrorCode.INVALID_ANSWER_FORMAT);
                }
            }
            case RADIO, DROPDOWN -> {
                List<Long> selected = answerCommand.selectedOptionIds();
                if (selected == null || selected.size() != 1) {
                    throw new FormDomainException(FormErrorCode.INVALID_VOTE_SELECTION);
                }
                validateOptionBelongsToQuestion(selected.get(0), question.getId());
            }
            case CHECKBOX -> {
                List<Long> selected = answerCommand.selectedOptionIds();
                if (selected == null || selected.isEmpty()) {
                    throw new FormDomainException(FormErrorCode.INVALID_VOTE_SELECTION);
                }
                for (Long optionId : selected) {
                    validateOptionBelongsToQuestion(optionId, question.getId());
                }
            }
            case FILE -> {
                List<String> fileIds = answerCommand.fileIds();
                if (fileIds == null || fileIds.isEmpty()) {
                    throw new FormDomainException(FormErrorCode.INVALID_ANSWER_FORMAT);
                }
                for (String fileId : fileIds) {
                    getFileUseCase.throwIfNotExists(fileId);
                }
            }
            case PORTFOLIO -> {
                String text = answerCommand.textValue();
                List<String> fileIds = answerCommand.fileIds();
                boolean hasText = text != null && !text.isBlank();
                boolean hasFiles = fileIds != null && !fileIds.isEmpty();
                if (!hasText && !hasFiles) {
                    throw new FormDomainException(FormErrorCode.INVALID_ANSWER_FORMAT);
                }
                if (hasFiles) {
                    for (String fileId : fileIds) {
                        getFileUseCase.throwIfNotExists(fileId);
                    }
                }
            }
            case SCHEDULE -> {
                List<Instant> times = answerCommand.times();
                if (times == null || times.isEmpty()) {
                    throw new FormDomainException(FormErrorCode.INVALID_ANSWER_FORMAT);
                }
                for (Instant t : times) {
                    if (!isAlignedToSlot(t)) {
                        throw new FormDomainException(FormErrorCode.INVALID_ANSWER_FORMAT);
                    }
                }
            }
        }
    }

    // 15분 = 900초. 슬롯 시작은 초 단위로 900의 배수이며 나노초 부분은 0.
    private static boolean isAlignedToSlot(Instant t) {
        return t.getEpochSecond() % 900 == 0 && t.getNano() == 0;
    }

    private void validateOptionBelongsToQuestion(Long optionId, Long questionId) {
        if (!loadQuestionOptionPort.existsByIdAndQuestionId(optionId, questionId)) {
            throw new FormDomainException(FormErrorCode.OPTION_NOT_IN_QUESTION);
        }
    }

    private List<AnswerWithOptions> buildAnswerData(FormResponse formResponse, List<AnswerCommand> answers) {
        List<Question> formQuestions = loadQuestionPort.listByFormId(formResponse.getForm().getId());

        List<AnswerWithOptions> result = new ArrayList<>();
        for (AnswerCommand answerCmd : answers) {
            Question question = formQuestions.stream()
                .filter(q -> q.getId().equals(answerCmd.questionId()))
                .findFirst()
                .orElseThrow(() -> new FormDomainException(FormErrorCode.QUESTION_NOT_FOUND));

            Set<String> fileIdSet = (answerCmd.fileIds() == null || answerCmd.fileIds().isEmpty())
                ? null
                : new HashSet<>(answerCmd.fileIds());
            Set<Instant> timeSet = (answerCmd.times() == null || answerCmd.times().isEmpty())
                ? null
                : new HashSet<>(answerCmd.times());
            Answer answer = Answer.create(
                formResponse,
                question,
                question.getType(),
                answerCmd.textValue(),
                fileIdSet,
                timeSet
            );

            List<QuestionOption> selectedOptions = new ArrayList<>();
            List<Long> optionIds = answerCmd.selectedOptionIds();
            if (optionIds != null && !optionIds.isEmpty()) {
                List<QuestionOption> options = loadQuestionOptionPort.listByQuestionId(question.getId());
                for (Long optionId : optionIds) {
                    QuestionOption option = options.stream()
                        .filter(o -> o.getId().equals(optionId))
                        .findFirst()
                        .orElseThrow(() -> new FormDomainException(FormErrorCode.OPTION_NOT_IN_QUESTION));
                    selectedOptions.add(option);
                }
            }

            result.add(new AnswerWithOptions(answer, selectedOptions));
        }
        return result;
    }

    /**
     * allowedQuestionIds 중 아직 답변되지 않은 질문에 대해 빈 Answer를 저장한다.
     * 제출 이후 해당 질문이 fork되더라도 Answer.questionId 역추적으로 질문을 복원하기 위함이다.
     */
    private void saveEmptyAnswersForUnanswered(
        FormResponse formResponse,
        Set<Long> allowedQuestionIds,
        Set<Long> answeredQuestionIds
    ) {
        if (allowedQuestionIds == null) {
            return;
        }

        Set<Long> unansweredIds = allowedQuestionIds.stream()
            .filter(id -> !answeredQuestionIds.contains(id))
            .collect(Collectors.toSet());

        if (unansweredIds.isEmpty()) {
            return;
        }

        List<Question> unansweredQuestions = loadQuestionPort.listByIdIn(unansweredIds);
        List<Answer> emptyAnswers = unansweredQuestions.stream()
            .map(q -> Answer.createEmpty(formResponse, q))
            .toList();

        saveAnswerPort.saveAll(emptyAnswers);
    }

    private void saveAnswers(List<AnswerWithOptions> data) {
        List<Answer> answers = data.stream().map(AnswerWithOptions::answer).toList();
        List<Answer> savedAnswers = saveAnswerPort.saveAll(answers);

        List<AnswerChoice> choices = new ArrayList<>();
        for (int i = 0; i < savedAnswers.size(); i++) {
            Answer savedAnswer = savedAnswers.get(i);
            for (QuestionOption option : data.get(i).options()) {
                choices.add(new AnswerChoice(savedAnswer, option));
            }
        }
        if (!choices.isEmpty()) {
            saveAnswerPort.saveAllChoices(choices);
        }
    }

    private record AnswerWithOptions(
        Answer answer,
        List<QuestionOption> options
    ) {
    }
}
