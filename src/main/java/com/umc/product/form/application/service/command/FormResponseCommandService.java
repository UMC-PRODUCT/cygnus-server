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
import com.umc.product.form.domain.exception.FormDomainException;
import com.umc.product.form.domain.exception.FormErrorCode;
import com.umc.product.global.exception.constant.Domain;
import com.umc.product.storage.application.port.in.query.GetFileUseCase;

import lombok.RequiredArgsConstructor;

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
        validateAllRequiredAnsweredOnPath(
            command.formId(),
            extractQuestionIds(command.answers()),
            extractSingleSelectedOptionIds(command.answers())
        );

        FormResponse response = FormResponse.createDraft(form, command.respondentMemberId());
        response.submit(Instant.now(), null);
        FormResponse saved = saveFormResponsePort.save(response);

        List<AnswerWithOptions> data = buildAnswerData(saved, command.answers());
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

        validateAnswers(command.formId(), command.answers());
        validateAllRequiredAnsweredOnPath(
            command.formId(),
            extractQuestionIds(command.answers()),
            extractSingleSelectedOptionIds(command.answers())
        );

        saveAnswerPort.deleteAllByFormResponseId(existing.getId());

        List<AnswerWithOptions> data = buildAnswerData(existing, command.answers());
        saveAnswers(data);

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
        Set<Long> answeredQuestionIds = savedAnswers.stream()
            .map(answer -> answer.getQuestion().getId())
            .collect(Collectors.toSet());

        if (command.allowedQuestionIds() != null) {
            validateAnsweredQuestionsAllowed(command.allowedQuestionIds(), answeredQuestionIds);
        }

        if (command.requiredQuestionIds() != null) {
            validateRequiredAnswered(command.requiredQuestionIds(), answeredQuestionIds);
        } else {
            // 이미 메모리에 있는 savedAnswers 로 (answerId → questionId) 매핑 미리 만들어서
            // AnswerChoice 순회 시 Answer 프록시 초기화(N+1) 회피.
            Map<Long, Long> answerIdToQuestionId = savedAnswers.stream()
                .collect(Collectors.toMap(Answer::getId, a -> a.getQuestion().getId()));
            Map<Long, Long> selectedOptionByQuestion = loadAnswerPort.listChoicesByAnswerIdIn(answerIdToQuestionId.keySet()).stream()
                .filter(c -> c.getQuestionOption() != null)
                .collect(Collectors.toMap(
                    c -> answerIdToQuestionId.get(c.getAnswer().getId()),
                    c -> c.getQuestionOption().getId(),
                    (a, b) -> a
                ));
            validateAllRequiredAnsweredOnPath(draft.getForm().getId(), answeredQuestionIds, selectedOptionByQuestion);
        }

        saveEmptyAnswersForUnanswered(draft, command.allowedQuestionIds(), answeredQuestionIds);

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
        validateAllRequiredAnsweredOnPath(
            command.formId(),
            extractQuestionIds(command.answers()),
            extractSingleSelectedOptionIds(command.answers())
        );

        String rawAccessKey = secureTokenGenerator.generateOpaqueToken();
        String accessKeyHash = secureTokenGenerator.sha256Hex(rawAccessKey);

        FormResponse response = FormResponse.createAnonymousDraft(form, accessKeyHash);
        response.submit(Instant.now(), null);
        FormResponse saved = saveFormResponsePort.save(response);

        List<AnswerWithOptions> data = buildAnswerData(saved, command.answers());
        saveAnswers(data);

        return AnonymousFormResponseResult.builder()
            .formResponseId(saved.getId())
            .responseAccessKey(rawAccessKey)
            .build();
    }

    @Override
    public void updateAnonymousResponse(UpdateAnonymousFormResponseCommand command) {
        FormResponse existing = loadSubmittedAsAnonymous(command.responseAccessKey());

        validateAnswers(existing.getForm().getId(), command.answers());
        validateAllRequiredAnsweredOnPath(
            existing.getForm().getId(),
            extractQuestionIds(command.answers()),
            extractSingleSelectedOptionIds(command.answers())
        );

        saveAnswerPort.deleteAllByFormResponseId(existing.getId());

        List<AnswerWithOptions> data = buildAnswerData(existing, command.answers());
        saveAnswers(data);

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
        Set<Long> answeredQuestionIds = savedAnswers.stream()
            .map(answer -> answer.getQuestion().getId())
            .collect(Collectors.toSet());

        if (command.allowedQuestionIds() != null) {
            validateAnsweredQuestionsAllowed(command.allowedQuestionIds(), answeredQuestionIds);
        }

        if (command.requiredQuestionIds() != null) {
            validateRequiredAnswered(command.requiredQuestionIds(), answeredQuestionIds);
        } else {
            // 이미 메모리에 있는 savedAnswers 로 (answerId → questionId) 매핑 미리 만들어서
            // AnswerChoice 순회 시 Answer 프록시 초기화(N+1) 회피.
            Map<Long, Long> answerIdToQuestionId = savedAnswers.stream()
                .collect(Collectors.toMap(Answer::getId, a -> a.getQuestion().getId()));
            Map<Long, Long> selectedOptionByQuestion = loadAnswerPort.listChoicesByAnswerIdIn(answerIdToQuestionId.keySet()).stream()
                .filter(c -> c.getQuestionOption() != null)
                .collect(Collectors.toMap(
                    c -> answerIdToQuestionId.get(c.getAnswer().getId()),
                    c -> c.getQuestionOption().getId(),
                    (a, b) -> a
                ));
            validateAllRequiredAnsweredOnPath(draft.getForm().getId(), answeredQuestionIds, selectedOptionByQuestion);
        }

        saveEmptyAnswersForUnanswered(draft, command.allowedQuestionIds(), answeredQuestionIds);

        draft.submit(Instant.now(), command.submittedIp());
        saveFormResponsePort.save(draft);
    }

    @Override
    public void deleteAnonymousDraft(DeleteAnonymousDraftFormResponseCommand command) {
        FormResponse draft = loadDraftAsAnonymous(command.responseAccessKey());

        saveAnswerPort.deleteAllByFormResponseId(draft.getId());
        saveFormResponsePort.deleteById(draft.getId());
    }

    /**
     * 응답 ID 로 DRAFT 응답 로드. 없으면 NOT_FOUND, DRAFT 가 아니면 NOT_DRAFT 예외.
     */
    private FormResponse loadDraft(Long formResponseId) {
        FormResponse formResponse = loadFormResponsePort.findById(formResponseId)
            .orElseThrow(() -> new FormDomainException(FormErrorCode.FORM_RESPONSE_NOT_FOUND));
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
        if (!form.isPublished()) {
            throw new FormDomainException(FormErrorCode.FORM_NOT_PUBLISHED);
        }
        return form;
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
        List<Question> formQuestions = loadQuestionPort.listByFormId(formId);
        for (Question q : formQuestions) {
            if (visitedSectionIds.contains(q.getFormSection().getId())
                && Boolean.TRUE.equals(q.getIsRequired())
                && !answeredQuestionIds.contains(q.getId())) {
                throw new FormDomainException(FormErrorCode.REQUIRED_QUESTION_NOT_ANSWERED);
            }
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

    private void validateRequiredAnswered(Set<Long> requiredQuestionIds, Set<Long> answeredQuestionIds) {
        for (Long questionId : requiredQuestionIds) {
            if (!answeredQuestionIds.contains(questionId)) {
                throw new FormDomainException(FormErrorCode.REQUIRED_QUESTION_NOT_ANSWERED);
            }
        }
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
            case SCHEDULE ->
                // 후속 PR 에서 지원
                throw new UnsupportedOperationException(
                    "Question type " + type + " is not supported yet");
        }
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
            Answer answer = Answer.create(
                formResponse,
                question,
                question.getType(),
                answerCmd.textValue(),
                fileIdSet
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
