package com.umc.product.form.application.service.query;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.authentication.application.service.SecureTokenGenerator;
import com.umc.product.form.application.port.in.FormActorContext;
import com.umc.product.form.application.port.in.query.GetAnswerUseCase;
import com.umc.product.form.application.port.in.query.dto.AnswerInfo;
import com.umc.product.form.application.port.out.LoadAnswerPort;
import com.umc.product.form.application.port.out.LoadFormResponsePort;
import com.umc.product.form.application.service.FormOwnershipAccessService;
import com.umc.product.form.domain.Answer;
import com.umc.product.form.domain.AnswerChoice;
import com.umc.product.form.domain.FormOperation;
import com.umc.product.form.domain.FormOwnerReference;
import com.umc.product.form.domain.FormResponse;
import com.umc.product.form.domain.exception.FormDomainException;
import com.umc.product.form.domain.exception.FormErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AnswerQueryService implements GetAnswerUseCase {

    private final LoadAnswerPort loadAnswerPort;
    private final LoadFormResponsePort loadFormResponsePort;
    private final FormOwnershipAccessService ownershipAccessService;
    // 익명 조회의 access-key 매칭용 (AnswerCommandService 와 동일 패턴)
    private final SecureTokenGenerator secureTokenGenerator;

    @Override
    public Optional<AnswerInfo> findById(
        FormOwnerReference expectedOwner,
        FormActorContext actorContext,
        Long answerId
    ) {
        requireExpectedOwnerRead(expectedOwner, actorContext);
        Optional<Answer> answer = loadAnswerPort.findById(answerId)
            .filter(value -> value.getFormResponse().getRespondentMemberId() != null);
        answer.ifPresent(value -> requireNamedRead(value.getFormResponse(), expectedOwner, actorContext));
        return answer.map(this::toAnswerInfo);
    }

    @Override
    public AnswerInfo getById(
        FormOwnerReference expectedOwner,
        FormActorContext actorContext,
        Long answerId
    ) {
        requireExpectedOwnerRead(expectedOwner, actorContext);
        Answer answer = loadAnswerPort.findById(answerId)
            .filter(a -> a.getFormResponse().getRespondentMemberId() != null)
            .orElseThrow(() -> new FormDomainException(FormErrorCode.ANSWER_NOT_FOUND));
        requireNamedRead(answer.getFormResponse(), expectedOwner, actorContext);
        return toAnswerInfo(answer);
    }

    @Override
    public List<AnswerInfo> listByFormResponseId(
        FormOwnerReference expectedOwner,
        FormActorContext actorContext,
        Long formResponseId
    ) {
        requireExpectedOwnerRead(expectedOwner, actorContext);
        Optional<FormResponse> response = loadFormResponsePort.findById(formResponseId);
        if (response.isEmpty() || response.get().getRespondentMemberId() == null) {
            return List.of();
        }
        requireSameResponse(response.get(), formResponseId, expectedOwner);
        requireNamedRead(response.get(), expectedOwner, actorContext);
        List<Answer> answers = loadAnswerPort.listByFormResponseId(formResponseId);
        if (answers.isEmpty()) {
            return List.of();
        }
        answers.forEach(answer -> requireSameResponse(
            answer.getFormResponse(), formResponseId, expectedOwner
        ));
        // 익명 응답의 답변은 노출 안 함 (기명 전용).
        if (answers.stream().anyMatch(answer ->
            answer.getFormResponse().getRespondentMemberId() == null)) {
            return List.of();
        }
        return buildAnswerInfos(answers);
    }

    @Override
    public Map<Long, List<AnswerInfo>> listByFormResponseIds(
        Collection<FormOwnerReference> expectedOwners,
        FormActorContext actorContext,
        Set<Long> formResponseIds
    ) {
        if (formResponseIds == null || formResponseIds.isEmpty()) {
            return Map.of();
        }
        FormOwnerScope ownerScope = FormOwnerScope.from(expectedOwners);
        ownerScope.requireNonEmpty();
        ownerScope.owners().forEach(owner -> requireExpectedOwnerRead(
            owner, actorContext
        ));

        List<FormResponse> responses = loadFormResponsePort.listByIdsWithForm(formResponseIds).stream()
            .filter(response -> response.getRespondentMemberId() != null)
            .toList();
        Set<Long> responsesReadFormIds = new HashSet<>();
        responses.forEach(response -> {
            requireRequestedResponse(response, formResponseIds);
            Long formId = response.getForm().getId();
            ownerScope.require(formId);
            if (!isRespondent(response, actorContext)) {
                responsesReadFormIds.add(formId);
            }
        });

        List<Answer> answers = loadAnswerPort.listByFormResponseIds(formResponseIds).stream()
            .filter(a -> a.getFormResponse().getRespondentMemberId() != null)
            .toList();
        answers.forEach(answer -> {
            FormResponse response = answer.getFormResponse();
            requireRequestedResponse(response, formResponseIds);
            Long formId = response.getForm().getId();
            ownerScope.require(formId);
            if (!isRespondent(response, actorContext)) {
                responsesReadFormIds.add(formId);
            }
        });
        responsesReadFormIds.forEach(formId -> ownershipAccessService.requireRead(
            formId,
            ownerScope.require(formId),
            actorContext,
            FormOperation.READ_RESPONSES
        ));
        if (answers.isEmpty()) {
            return Map.of();
        }

        Set<Long> answerIds = answers.stream()
            .map(Answer::getId)
            .collect(Collectors.toSet());
        List<AnswerChoice> allChoices = loadAnswerPort.listChoicesByAnswerIdIn(answerIds);
        Map<Long, List<AnswerChoice>> choicesByAnswer = allChoices.stream()
            .collect(Collectors.groupingBy(c -> c.getAnswer().getId()));

        return answers.stream()
            .collect(Collectors.groupingBy(
                answer -> answer.getFormResponse().getId(),
                Collectors.mapping(
                    answer -> AnswerInfo.from(answer, choicesByAnswer.getOrDefault(answer.getId(), List.of())),
                    Collectors.toList()
                )
            ));
    }

    @Override
    public Optional<AnswerInfo> findByIdAsAnonymous(
        FormOwnerReference expectedOwner,
        FormActorContext actorContext,
        Long answerId
    ) {
        requireExpectedOwnerRead(expectedOwner, actorContext);
        String responseAccessKey = responseAccessKey(actorContext);
        // 인증 실패 유출 방지 — 익명/hash 검증 실패 시 silently empty
        Optional<Answer> answer = loadAnswerPort.findById(answerId)
            .filter(value -> isAuthorizedAnonymous(value.getFormResponse(), responseAccessKey));
        answer.ifPresent(value -> requireAnonymousRead(value.getFormResponse(), expectedOwner, actorContext));
        return answer.map(this::toAnswerInfo);
    }

    @Override
    public AnswerInfo getByIdAsAnonymous(
        FormOwnerReference expectedOwner,
        FormActorContext actorContext,
        Long answerId
    ) {
        requireExpectedOwnerRead(expectedOwner, actorContext);
        String responseAccessKey = responseAccessKey(actorContext);
        // 익명 경계 유출 방지 — 답변 없음 / 기명 응답 / hash 불일치 모두 FORBIDDEN 으로 통일
        Answer answer = loadAnswerPort.findById(answerId)
            .orElseThrow(() -> new FormDomainException(FormErrorCode.FORM_RESPONSE_FORBIDDEN));
        if (!isAuthorizedAnonymous(answer.getFormResponse(), responseAccessKey)) {
            throw new FormDomainException(FormErrorCode.FORM_RESPONSE_FORBIDDEN);
        }
        requireAnonymousRead(answer.getFormResponse(), expectedOwner, actorContext);
        return toAnswerInfo(answer);
    }

    @Override
    public List<AnswerInfo> listByFormResponseIdAsAnonymous(
        FormOwnerReference expectedOwner,
        FormActorContext actorContext,
        Long formResponseId
    ) {
        requireExpectedOwnerRead(expectedOwner, actorContext);
        String responseAccessKey = responseAccessKey(actorContext);
        Optional<FormResponse> response = loadFormResponsePort.findById(formResponseId);
        if (response.isEmpty()) {
            return List.of();
        }
        if (!isAuthorizedAnonymous(response.get(), responseAccessKey)) {
            throw new FormDomainException(FormErrorCode.FORM_RESPONSE_FORBIDDEN);
        }
        requireSameResponse(response.get(), formResponseId, expectedOwner);
        requireAnonymousRead(response.get(), expectedOwner, actorContext);
        List<Answer> answers = loadAnswerPort.listByFormResponseId(formResponseId);
        if (answers.isEmpty()) {
            // 응답 존재 여부 유출 방지 — 빈 리스트로 통일
            return List.of();
        }
        answers.forEach(answer -> {
            requireSameResponse(answer.getFormResponse(), formResponseId, expectedOwner);
            if (!isAuthorizedAnonymous(answer.getFormResponse(), responseAccessKey)) {
                throw new FormDomainException(FormErrorCode.FORM_RESPONSE_FORBIDDEN);
            }
        });
        return buildAnswerInfos(answers);
    }

    /**
     * FormResponse 가 익명이고 저장된 hash 가 rawKey 의 sha256 과 일치하는지 확인.
     */
    private boolean isAuthorizedAnonymous(FormResponse response, String rawAccessKey) {
        if (response.getRespondentMemberId() != null) {
            return false;
        }
        String hash = secureTokenGenerator.sha256Hex(rawAccessKey);
        return hash.equals(response.getResponseAccessKeyHash());
    }

    private void requireNamedRead(
        FormResponse response,
        FormOwnerReference expectedOwner,
        FormActorContext actorContext
    ) {
        FormOperation operation = isRespondent(response, actorContext)
            ? FormOperation.READ
            : FormOperation.READ_RESPONSES;
        ownershipAccessService.requireRead(
            response.getForm().getId(), expectedOwner, actorContext, operation
        );
    }

    private void requireAnonymousRead(
        FormResponse response,
        FormOwnerReference expectedOwner,
        FormActorContext actorContext
    ) {
        ownershipAccessService.requireRead(
            response.getForm().getId(), expectedOwner, actorContext, FormOperation.READ
        );
    }

    private void requireExpectedOwnerRead(
        FormOwnerReference expectedOwner,
        FormActorContext actorContext
    ) {
        Long formId = expectedOwner == null ? null : expectedOwner.formId();
        ownershipAccessService.requireRead(
            formId, expectedOwner, actorContext, FormOperation.READ
        );
    }

    private static void requireSameResponse(
        FormResponse response,
        Long expectedResponseId,
        FormOwnerReference expectedOwner
    ) {
        if (expectedResponseId == null
            || response.getId() == null
            || !expectedResponseId.equals(response.getId())
            || response.getForm().getId() == null
            || expectedOwner == null
            || !response.getForm().getId().equals(expectedOwner.formId())) {
            throw new FormDomainException(FormErrorCode.FORM_OWNERSHIP_FORBIDDEN);
        }
    }

    private static boolean isRespondent(
        FormResponse response,
        FormActorContext actorContext
    ) {
        return actorContext != null
            && actorContext.authenticatedMemberId()
                .filter(memberId -> memberId.equals(response.getRespondentMemberId()))
                .isPresent();
    }

    private static void requireRequestedResponse(
        FormResponse response,
        Set<Long> expectedResponseIds
    ) {
        if (response.getId() == null || !expectedResponseIds.contains(response.getId())) {
            throw new FormDomainException(FormErrorCode.FORM_OWNERSHIP_FORBIDDEN);
        }
    }

    private static String responseAccessKey(FormActorContext actorContext) {
        if (actorContext == null) {
            throw new FormDomainException(FormErrorCode.RESPONSE_ACCESS_KEY_REQUIRED);
        }
        return actorContext.responseAccessKey()
            .orElseThrow(() -> new FormDomainException(FormErrorCode.RESPONSE_ACCESS_KEY_REQUIRED));
    }

    /**
     * 답변 목록에서 AnswerChoice 를 벌크 로드해 AnswerInfo 로 조립.
     */
    private List<AnswerInfo> buildAnswerInfos(List<Answer> answers) {
        Set<Long> answerIds = answers.stream()
            .map(Answer::getId)
            .collect(Collectors.toSet());
        List<AnswerChoice> allChoices = loadAnswerPort.listChoicesByAnswerIdIn(answerIds);
        Map<Long, List<AnswerChoice>> choicesByAnswer = allChoices.stream()
            .collect(Collectors.groupingBy(c -> c.getAnswer().getId()));

        return answers.stream()
            .map(answer -> AnswerInfo.from(
                answer,
                choicesByAnswer.getOrDefault(answer.getId(), List.of())
            ))
            .toList();
    }

    /**
     * 단건 Answer -> AnswerInfo. 해당 답변의 AnswerChoice 만 조회 후 조립.
     */
    private AnswerInfo toAnswerInfo(Answer answer) {
        List<AnswerChoice> choices = loadAnswerPort.listChoicesByAnswerIdIn(Set.of(answer.getId()));
        return AnswerInfo.from(answer, choices);
    }
}
