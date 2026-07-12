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
import com.umc.product.form.application.port.in.command.ManageFormResponseUseCase;
import com.umc.product.form.application.port.in.command.dto.AnswerCommand;
import com.umc.product.form.application.port.in.command.dto.CreateDraftFormResponseCommand;
import com.umc.product.form.application.port.in.command.dto.DeleteDraftFormResponseCommand;
import com.umc.product.form.application.port.in.command.dto.DeleteFormResponseCommand;
import com.umc.product.form.application.port.in.command.dto.SubmitDraftFormResponseCommand;
import com.umc.product.form.application.port.in.command.dto.SubmitFormResponseCommand;
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
            Set<Long> answerIds = savedAnswers.stream().map(Answer::getId).collect(Collectors.toSet());
            Map<Long, Long> selectedOptionByQuestion = loadAnswerPort.listChoicesByAnswerIdIn(answerIds).stream()
                .filter(c -> c.getQuestionOption() != null)
                .collect(Collectors.toMap(
                    c -> c.getAnswer().getQuestion().getId(),
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
