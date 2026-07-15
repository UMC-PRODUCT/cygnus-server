package com.umc.product.form.application.service.command;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.authentication.application.service.SecureTokenGenerator;
import com.umc.product.form.application.port.in.command.ManageAnswerUseCase;
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
import com.umc.product.form.domain.AnswerChoice;
import com.umc.product.form.domain.FormResponse;
import com.umc.product.form.domain.Question;
import com.umc.product.form.domain.QuestionOption;
import com.umc.product.form.domain.enums.FormResponseStatus;
import com.umc.product.form.domain.enums.QuestionType;
import com.umc.product.form.domain.exception.FormDomainException;
import com.umc.product.form.domain.exception.FormErrorCode;
import com.umc.product.storage.application.port.in.query.GetFileUseCase;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class AnswerCommandService implements ManageAnswerUseCase {

    private final LoadFormResponsePort loadFormResponsePort;
    private final LoadQuestionPort loadQuestionPort;
    private final LoadQuestionOptionPort loadQuestionOptionPort;
    private final LoadAnswerPort loadAnswerPort;
    private final SaveAnswerPort saveAnswerPort;
    private final SaveFormResponsePort saveFormResponsePort;
    private final GetFileUseCase getFileUseCase;
    // authentication 도메인의 crypto util
    // 재배치(common/security 등) 는 별도 리팩터 PR 대상.
    private final SecureTokenGenerator secureTokenGenerator;

    @Override
    public Long createAnswer(CreateAnswerCommand command) {
        FormResponse draft = loadDraftAsOwner(command.formResponseId(), command.requesterMemberId());
        Question question = loadQuestionInForm(command.questionId(), draft.getForm().getId());

        // 같은 질문에 대한 답변이 이미 있으면 예외
        if (loadAnswerPort.existsByFormResponseIdAndQuestionId(draft.getId(), question.getId())) {
            throw new FormDomainException(FormErrorCode.ANSWER_ALREADY_EXISTS);
        }

        validateAnswerContent(question, command.textValue(), command.selectedOptionIds(), command.fileIds());

        Answer answer = Answer.create(
            draft, question, question.getType(),
            command.textValue(),
            toFileIdSet(command.fileIds())
        );
        Answer saved = saveAnswerPort.save(answer);

        // 객관식이면 AnswerChoice도 같이 저장
        List<AnswerChoice> choices = buildChoices(saved, question, command.selectedOptionIds());
        if (!choices.isEmpty()) {
            saveAnswerPort.saveAllChoices(choices);
        }

        draft.updateLastSavedAt(Instant.now());
        saveFormResponsePort.save(draft);

        return saved.getId();
    }

    @Override
    public void updateAnswer(UpdateAnswerCommand command) {
        Answer existing = loadAnswerAndDraftAsOwner(command.answerId(), command.requesterMemberId());
        FormResponse draft = existing.getFormResponse();
        Question question = existing.getQuestion();
        validateAnswerContent(question, command.textValue(), command.selectedOptionIds(), command.fileIds());

        // 1. 기존 AnswerChoice 만 삭제 (Answer 는 PK 유지하며 update)
        saveAnswerPort.deleteChoicesByAnswerId(existing.getId());

        // 2. Answer 의 textValue / fileIds 갱신 (PATCH 시맨틱 — null 은 기존 값 유지)
        Set<String> requestedFileIds = command.fileIds() == null
            ? null  // null = keep
            : new HashSet<>(command.fileIds());  // empty = clear, non-empty = set
        existing.update(command.textValue(), requestedFileIds);
        saveAnswerPort.save(existing);

        // 3. 새 AnswerChoice 저장 (객관식인 경우)
        List<AnswerChoice> choices = buildChoices(existing, question, command.selectedOptionIds());
        if (!choices.isEmpty()) {
            saveAnswerPort.saveAllChoices(choices);
        }

        draft.updateLastSavedAt(Instant.now());
        saveFormResponsePort.save(draft);
    }

    @Override
    public void deleteAnswer(DeleteAnswerCommand command) {
        Answer existing = loadAnswerAndDraftAsOwner(command.answerId(), command.requesterMemberId());
        FormResponse draft = existing.getFormResponse();

        saveAnswerPort.deleteByAnswerId(existing.getId());
        draft.updateLastSavedAt(Instant.now());
        saveFormResponsePort.save(draft);
    }

    @Override
    public Long createAnonymousAnswer(CreateAnonymousAnswerCommand command) {
        FormResponse draft = loadDraftAsAnonymous(command.responseAccessKey());
        Question question = loadQuestionInForm(command.questionId(), draft.getForm().getId());

        // 같은 질문에 대한 답변이 이미 있으면 예외
        if (loadAnswerPort.existsByFormResponseIdAndQuestionId(draft.getId(), question.getId())) {
            throw new FormDomainException(FormErrorCode.ANSWER_ALREADY_EXISTS);
        }

        validateAnswerContent(question, command.textValue(), command.selectedOptionIds(), command.fileIds());

        Answer answer = Answer.create(
            draft, question, question.getType(),
            command.textValue(),
            toFileIdSet(command.fileIds())
        );
        Answer saved = saveAnswerPort.save(answer);

        // 객관식이면 AnswerChoice도 같이 저장
        List<AnswerChoice> choices = buildChoices(saved, question, command.selectedOptionIds());
        if (!choices.isEmpty()) {
            saveAnswerPort.saveAllChoices(choices);
        }

        draft.updateLastSavedAt(Instant.now());
        saveFormResponsePort.save(draft);

        return saved.getId();
    }

    @Override
    public void updateAnonymousAnswer(UpdateAnonymousAnswerCommand command) {
        Answer existing = loadAnswerAndDraftAsAnonymous(command.answerId(), command.responseAccessKey());
        FormResponse draft = existing.getFormResponse();
        Question question = existing.getQuestion();
        validateAnswerContent(question, command.textValue(), command.selectedOptionIds(), command.fileIds());

        // 1. 기존 AnswerChoice 만 삭제 (Answer 는 PK 유지하며 update)
        saveAnswerPort.deleteChoicesByAnswerId(existing.getId());

        // 2. Answer 의 textValue / fileIds 갱신 (PATCH 시맨틱 — null 은 기존 값 유지)
        Set<String> requestedFileIds = command.fileIds() == null
            ? null  // null = keep
            : new HashSet<>(command.fileIds());  // empty = clear, non-empty = set
        existing.update(command.textValue(), requestedFileIds);
        saveAnswerPort.save(existing);

        // 3. 새 AnswerChoice 저장 (객관식인 경우)
        List<AnswerChoice> choices = buildChoices(existing, question, command.selectedOptionIds());
        if (!choices.isEmpty()) {
            saveAnswerPort.saveAllChoices(choices);
        }

        draft.updateLastSavedAt(Instant.now());
        saveFormResponsePort.save(draft);
    }

    @Override
    public void deleteAnonymousAnswer(DeleteAnonymousAnswerCommand command) {
        Answer existing = loadAnswerAndDraftAsAnonymous(command.answerId(), command.responseAccessKey());
        FormResponse draft = existing.getFormResponse();

        saveAnswerPort.deleteByAnswerId(existing.getId());
        draft.updateLastSavedAt(Instant.now());
        saveFormResponsePort.save(draft);
    }

    /**
     * 응답 ID로 DRAFT 응답 로드. 없으면 NOT_FOUND, DRAFT가 아니면 NOT_DRAFT 예외.
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
     * 소유자 검증까지 포함한 DRAFT 응답 로드 (기명 전용). {@code FormResponseCommandService.loadDraftAsOwner} 와 대칭.
     * <p>
     * 순서: DRAFT 로드 → 익명/소유자 대조.
     * 다음 경우 모두 {@link FormErrorCode#FORM_RESPONSE_FORBIDDEN} 처리:
     * <ul>
     *   <li>익명 draft({@code respondentMemberId=null}) — 기명 UseCase 로 접근 불가, 익명 UseCase 사용</li>
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

    /**
     * answerId 로 Answer 를 로드하고 그 FormResponse 에 대한 소유자 검증까지 수행 (기명 전용).
     * <p>
     * Answer 없으면 {@link FormErrorCode#ANSWER_NOT_FOUND}. FormResponse 가 DRAFT 아니거나 소유자 불일치/익명이면
     * {@link #loadDraftAsOwner} 규칙에 따라 예외.
     */
    private Answer loadAnswerAndDraftAsOwner(Long answerId, Long requesterMemberId) {
        Answer existing = loadAnswerPort.findById(answerId)
            .orElseThrow(() -> new FormDomainException(FormErrorCode.ANSWER_NOT_FOUND));
        FormResponse draft = existing.getFormResponse();
        if (draft.getStatus() != FormResponseStatus.DRAFT) {
            throw new FormDomainException(FormErrorCode.FORM_RESPONSE_NOT_DRAFT);
        }
        if (draft.getRespondentMemberId() == null
            || !draft.getRespondentMemberId().equals(requesterMemberId)) {
            throw new FormDomainException(FormErrorCode.FORM_RESPONSE_FORBIDDEN);
        }
        return existing;
    }

    /**
     * 익명 draft 응답 로드 + 검증 (익명 전용). {@code FormResponseCommandService.loadDraftAsAnonymous} 와 대칭.
     * <p>
     * rawKey null → {@link FormErrorCode#RESPONSE_ACCESS_KEY_REQUIRED}.
     * hash 매칭 실패 / 기명 draft → {@link FormErrorCode#FORM_RESPONSE_FORBIDDEN}.
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
     * answerId 로 Answer 로드 + 그 FormResponse 가 익명 draft 인지 및 access key hash 매칭 검증 (익명 전용).
     * <p>
     * 익명 경계 유출 방지를 위해 {@code FormResponseCommandService.loadDraftAsAnonymous} 와 동일하게
     * rawKey null 을 제외한 모든 실패 케이스는 {@link FormErrorCode#FORM_RESPONSE_FORBIDDEN} 로 통일한다
     * (Answer 존재 여부 / DRAFT 여부 / 기명 여부 / hash 매칭 결과 어느 것도 응답 코드로 유출하지 않음).
     */
    private Answer loadAnswerAndDraftAsAnonymous(Long answerId, String rawAccessKey) {
        if (rawAccessKey == null) {
            throw new FormDomainException(FormErrorCode.RESPONSE_ACCESS_KEY_REQUIRED);
        }
        Answer existing = loadAnswerPort.findById(answerId)
            .orElseThrow(() -> new FormDomainException(FormErrorCode.FORM_RESPONSE_FORBIDDEN));
        FormResponse draft = existing.getFormResponse();
        if (draft.getStatus() != FormResponseStatus.DRAFT
            || draft.getRespondentMemberId() != null) {
            throw new FormDomainException(FormErrorCode.FORM_RESPONSE_FORBIDDEN);
        }
        String hash = secureTokenGenerator.sha256Hex(rawAccessKey);
        if (!hash.equals(draft.getResponseAccessKeyHash())) {
            throw new FormDomainException(FormErrorCode.FORM_RESPONSE_FORBIDDEN);
        }
        return existing;
    }

    /**
     * 질문 로드 + 해당 폼 소속 검증.
     */
    private Question loadQuestionInForm(Long questionId, Long formId) {
        Question question = loadQuestionPort.findById(questionId)
            .orElseThrow(() -> new FormDomainException(FormErrorCode.QUESTION_NOT_FOUND));
        if (!question.getFormSection().getForm().getId().equals(formId)) {
            throw new FormDomainException(FormErrorCode.QUESTION_IS_NOT_OWNED_BY_FORM);
        }
        return question;
    }

    /**
     * 질문 type 별 답변 형식 검증.
     */
    private void validateAnswerContent(
        Question question,
        String textValue,
        List<Long> selectedOptionIds,
        List<String> fileIds
    ) {
        switch (question.getType()) {
            case SHORT_TEXT, LONG_TEXT -> {
                if (textValue == null || textValue.isBlank()) {
                    throw new FormDomainException(FormErrorCode.INVALID_ANSWER_FORMAT);
                }
            }
            case RADIO, DROPDOWN -> {
                if (selectedOptionIds == null || selectedOptionIds.size() != 1) {
                    throw new FormDomainException(FormErrorCode.INVALID_VOTE_SELECTION);
                }
                validateOptionBelongsToQuestion(selectedOptionIds.get(0), question.getId());
            }
            case CHECKBOX -> {
                if (selectedOptionIds == null || selectedOptionIds.isEmpty()) {
                    throw new FormDomainException(FormErrorCode.INVALID_VOTE_SELECTION);
                }
                for (Long optionId : selectedOptionIds) {
                    validateOptionBelongsToQuestion(optionId, question.getId());
                }
            }
            case FILE -> {
                if (fileIds == null || fileIds.isEmpty()) {
                    throw new FormDomainException(FormErrorCode.INVALID_ANSWER_FORMAT);
                }
                for (String fileId : fileIds) {
                    getFileUseCase.throwIfNotExists(fileId);
                }
            }
            case PORTFOLIO -> {
                boolean hasText = textValue != null && !textValue.isBlank();
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
                    "Question type " + question.getType() + " is not supported yet");
        }
    }

    private void validateOptionBelongsToQuestion(Long optionId, Long questionId) {
        if (!loadQuestionOptionPort.existsByIdAndQuestionId(optionId, questionId)) {
            throw new FormDomainException(FormErrorCode.OPTION_NOT_IN_QUESTION);
        }
    }

    /**
     * fileIds List를 Set 으로 변환. null이면 null 반환 (Answer.fileIds 도 null 허용 컬럼).
     */
    private static Set<String> toFileIdSet(List<String> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return null;
        }
        return new HashSet<>(fileIds);
    }

    /**
     * 객관식 답변의 AnswerChoice 들을 빌드. 객관식이 아닌 type은 빈 리스트.
     */
    private List<AnswerChoice> buildChoices(Answer answer, Question question, List<Long> selectedOptionIds) {
        if (selectedOptionIds == null || selectedOptionIds.isEmpty()) {
            return List.of();
        }
        QuestionType type = question.getType();
        if (type != QuestionType.RADIO && type != QuestionType.CHECKBOX && type != QuestionType.DROPDOWN) {
            return List.of();
        }

        List<QuestionOption> options = loadQuestionOptionPort.listByQuestionId(question.getId());
        List<AnswerChoice> choices = new ArrayList<>();
        for (Long optionId : selectedOptionIds) {
            QuestionOption option = options.stream()
                .filter(o -> o.getId().equals(optionId))
                .findFirst()
                .orElseThrow(() -> new FormDomainException(FormErrorCode.OPTION_NOT_IN_QUESTION));
            choices.add(new AnswerChoice(answer, option));
        }
        return choices;
    }
}
