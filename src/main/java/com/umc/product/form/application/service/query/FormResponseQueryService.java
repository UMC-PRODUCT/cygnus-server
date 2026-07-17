package com.umc.product.form.application.service.query;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.authentication.application.service.SecureTokenGenerator;
import com.umc.product.form.application.port.in.FormActorContext;
import com.umc.product.form.application.port.in.query.GetAnswerUseCase;
import com.umc.product.form.application.port.in.query.GetFormResponseUseCase;
import com.umc.product.form.application.port.in.query.dto.AnswerInfo;
import com.umc.product.form.application.port.in.query.dto.FormResponseInfo;
import com.umc.product.form.application.port.in.query.dto.FormResponseWithAnswersInfo;
import com.umc.product.form.application.port.out.LoadFormResponsePort;
import com.umc.product.form.application.service.FormOwnershipAccessService;
import com.umc.product.form.domain.FormOperation;
import com.umc.product.form.domain.FormOwnerReference;
import com.umc.product.form.domain.FormResponse;
import com.umc.product.form.domain.exception.FormDomainException;
import com.umc.product.form.domain.exception.FormErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FormResponseQueryService implements GetFormResponseUseCase {

    private final LoadFormResponsePort loadFormResponsePort;
    private final GetAnswerUseCase getAnswerUseCase;
    private final FormOwnershipAccessService ownershipAccessService;
    // authentication 도메인의 공용 crypto util 재사용 (SSO Auth Code 발급과 동일 패턴).
    // 재배치(common/security 등) 는 별도 리팩터 PR 대상.
    private final SecureTokenGenerator secureTokenGenerator;

    @Override
    public Optional<FormResponseInfo> findById(
        FormOwnerReference expectedOwner,
        FormActorContext actorContext,
        Long formResponseId
    ) {
        requireExpectedOwnerRead(expectedOwner, actorContext);
        Optional<FormResponse> response = loadFormResponsePort.findById(formResponseId);
        response.ifPresent(value -> requireResponseRead(value, expectedOwner, actorContext));
        return response.map(FormResponseInfo::from);
    }

    @Override
    public FormResponseInfo getById(
        FormOwnerReference expectedOwner,
        FormActorContext actorContext,
        Long formResponseId
    ) {
        requireExpectedOwnerRead(expectedOwner, actorContext);
        FormResponse response = loadFormResponsePort.findById(formResponseId)
            .orElseThrow(() -> new FormDomainException(FormErrorCode.FORM_RESPONSE_NOT_FOUND));
        requireResponseRead(response, expectedOwner, actorContext);
        return FormResponseInfo.from(response);
    }

    @Override
    public List<FormResponseInfo> listByFormId(
        FormOwnerReference expectedOwner,
        FormActorContext actorContext
    ) {
        Long formId = expectedOwner == null ? null : expectedOwner.formId();
        requireResponsesRead(formId, expectedOwner, actorContext);
        List<FormResponse> responses = loadFormResponsePort.listByFormId(formId);
        responses.forEach(response -> requireSameRoot(response.getForm().getId(), expectedOwner));
        return responses.stream()
            .map(FormResponseInfo::from)
            .toList();
    }

    @Override
    public List<FormResponseInfo> listSubmittedByFormId(
        FormOwnerReference expectedOwner,
        FormActorContext actorContext
    ) {
        Long formId = expectedOwner == null ? null : expectedOwner.formId();
        requireResponsesRead(formId, expectedOwner, actorContext);
        List<FormResponse> responses = loadFormResponsePort.listSubmittedByFormId(formId);
        responses.forEach(response -> requireSameRoot(response.getForm().getId(), expectedOwner));
        return responses.stream()
            .map(FormResponseInfo::from)
            .toList();
    }

    @Override
    public List<FormResponseInfo> listDraftByRespondent(
        Collection<FormOwnerReference> expectedOwners,
        FormActorContext actorContext
    ) {
        FormOwnerScope ownerScope = FormOwnerScope.from(expectedOwners);
        if (ownerScope.isEmpty()) {
            return List.of();
        }
        Long respondentMemberId = requireRespondentMemberId(actorContext);
        ownerScope.owners().forEach(owner -> requireRead(
            owner.formId(), owner, actorContext
        ));
        List<FormResponse> drafts = loadFormResponsePort.findAllDraftByRespondentMemberId(respondentMemberId);
        List<FormResponse> scopedDrafts = drafts.stream()
            .filter(draft -> respondentMemberId.equals(draft.getRespondentMemberId()))
            .filter(draft -> ownerScope.contains(draft.getForm().getId()))
            .toList();
        scopedDrafts.forEach(draft -> ownerScope.require(draft.getForm().getId()));
        return scopedDrafts.stream()
            .map(FormResponseInfo::from)
            .toList();
    }

    @Override
    public Optional<FormResponseInfo> findDraftByRespondent(
        FormOwnerReference expectedOwner,
        FormActorContext actorContext
    ) {
        Long respondentMemberId = requireRespondentMemberId(actorContext);
        Long formId = expectedOwner == null ? null : expectedOwner.formId();
        requireRead(formId, expectedOwner, actorContext);
        Optional<FormResponse> response = loadFormResponsePort.findDraftByFormIdAndRespondentMemberId(
            formId, respondentMemberId
        );
        response.ifPresent(value -> requireRespondentResponse(
            value, respondentMemberId, expectedOwner
        ));
        return response
            .map(FormResponseInfo::from);
    }

    @Override
    public Optional<FormResponseInfo> findSubmittedByRespondent(
        FormOwnerReference expectedOwner,
        FormActorContext actorContext
    ) {
        Long respondentMemberId = requireRespondentMemberId(actorContext);
        Long formId = expectedOwner == null ? null : expectedOwner.formId();
        requireRead(formId, expectedOwner, actorContext);
        Optional<FormResponse> response = loadFormResponsePort.findSubmittedByFormIdAndRespondentMemberId(
            formId, respondentMemberId
        );
        response.ifPresent(value -> requireRespondentResponse(
            value, respondentMemberId, expectedOwner
        ));
        return response
            .map(FormResponseInfo::from);
    }

    @Override
    public FormResponseWithAnswersInfo getResponseWithAnswers(
        FormOwnerReference expectedOwner,
        FormActorContext actorContext,
        Long formResponseId
    ) {
        return findResponseWithAnswers(expectedOwner, actorContext, formResponseId)
            .orElseThrow(() -> new FormDomainException(FormErrorCode.FORM_RESPONSE_NOT_FOUND));
    }

    @Override
    public Optional<FormResponseWithAnswersInfo> findResponseWithAnswers(
        FormOwnerReference expectedOwner,
        FormActorContext actorContext,
        Long formResponseId
    ) {
        requireExpectedOwnerRead(expectedOwner, actorContext);
        Optional<FormResponse> response = loadFormResponsePort.findById(formResponseId)
            .filter(value -> value.getRespondentMemberId() != null);
        response.ifPresent(value -> requireResponseRead(value, expectedOwner, actorContext));
        return response
            .map(formResponse -> FormResponseWithAnswersInfo.from(
                formResponse,
                getAnswerUseCase.listByFormResponseId(expectedOwner, actorContext, formResponseId)
            ));
    }

    private static Long requireRespondentMemberId(FormActorContext actorContext) {
        if (actorContext == null || actorContext.authenticatedMemberId().isEmpty()) {
            throw new FormDomainException(FormErrorCode.RESPONDENT_MEMBER_ID_REQUIRED);
        }
        return actorContext.authenticatedMemberId().orElseThrow();
    }

    @Override
    public Optional<FormResponseInfo> findByAccessKey(
        FormOwnerReference expectedOwner,
        FormActorContext actorContext
    ) {
        requireExpectedOwnerRead(expectedOwner, actorContext);
        Optional<FormResponse> response = loadAnonymousResponseByAccessKey(actorContext);
        response.ifPresent(value -> requireRead(value.getForm().getId(), expectedOwner, actorContext));
        return response.map(FormResponseInfo::from);
    }

    @Override
    public FormResponseWithAnswersInfo getResponseWithAnswersByAccessKey(
        FormOwnerReference expectedOwner,
        FormActorContext actorContext
    ) {
        requireExpectedOwnerRead(expectedOwner, actorContext);
        FormResponse response = loadAnonymousResponseByAccessKey(actorContext)
            .orElseThrow(() -> new FormDomainException(FormErrorCode.FORM_RESPONSE_NOT_FOUND));
        requireRead(response.getForm().getId(), expectedOwner, actorContext);
        return FormResponseWithAnswersInfo.from(
            response,
            getAnswerUseCase.listByFormResponseIdAsAnonymous(
                expectedOwner, actorContext, response.getId()
            )
        );
    }

    /**
     * 익명 응답 조회 헬퍼 — rawKey 를 sha256 뜨고 hash 매칭.
     * 기명 응답이 조회되면 방어 목적으로 empty.
     * <p>
     * rawKey 가 null 이면 {@link FormErrorCode#RESPONSE_ACCESS_KEY_REQUIRED}.
     */
    private Optional<FormResponse> loadAnonymousResponseByAccessKey(FormActorContext actorContext) {
        String rawKey = responseAccessKey(actorContext);
        String hash = secureTokenGenerator.sha256Hex(rawKey);
        return loadFormResponsePort.findByAccessKeyHash(hash)
            .filter(fr -> fr.getRespondentMemberId() == null);
    }

    @Override
    public Map<Long, FormResponseWithAnswersInfo> findResponsesWithAnswers(
        Collection<FormOwnerReference> expectedOwners,
        FormActorContext actorContext,
        Set<Long> formResponseIds
    ) {
        if (formResponseIds == null || formResponseIds.isEmpty()) {
            return Map.of();
        }
        FormOwnerScope ownerScope = FormOwnerScope.from(expectedOwners);
        ownerScope.requireNonEmpty();
        ownerScope.owners().forEach(owner -> requireRead(
            owner.formId(), owner, actorContext
        ));

        List<FormResponse> formResponses = loadFormResponsePort.listByIdsWithForm(formResponseIds).stream()
            .filter(fr -> fr.getRespondentMemberId() != null)
            .toList();
        Set<Long> responsesReadFormIds = new HashSet<>();
        formResponses.forEach(response -> {
            requireRequestedResponse(response, formResponseIds);
            Long formId = response.getForm().getId();
            ownerScope.require(formId);
            if (!isRespondent(response, actorContext)) {
                responsesReadFormIds.add(formId);
            }
        });
        responsesReadFormIds.forEach(formId -> requireResponsesRead(
            formId, ownerScope.require(formId), actorContext
        ));
        Map<Long, List<AnswerInfo>> answersByFormResponseId =
            getAnswerUseCase.listByFormResponseIds(
                ownerScope.owners(), actorContext, formResponseIds
            );

        return formResponses.stream()
            .map(formResponse -> FormResponseWithAnswersInfo.from(
                formResponse,
                answersByFormResponseId.getOrDefault(formResponse.getId(), List.of())
            ))
            .collect(Collectors.toMap(
                FormResponseWithAnswersInfo::id,
                Function.identity()
            ));
    }

    private void requireResponseRead(
        FormResponse response,
        FormOwnerReference expectedOwner,
        FormActorContext actorContext
    ) {
        if (isRespondent(response, actorContext) || response.getRespondentMemberId() == null) {
            requireRead(response.getForm().getId(), expectedOwner, actorContext);
            return;
        }
        requireResponsesRead(response.getForm().getId(), expectedOwner, actorContext);
    }

    private void requireRead(
        Long formId,
        FormOwnerReference expectedOwner,
        FormActorContext actorContext
    ) {
        ownershipAccessService.requireRead(
            formId, expectedOwner, actorContext, FormOperation.READ
        );
    }

    private void requireResponsesRead(
        Long formId,
        FormOwnerReference expectedOwner,
        FormActorContext actorContext
    ) {
        ownershipAccessService.requireRead(
            formId, expectedOwner, actorContext, FormOperation.READ_RESPONSES
        );
    }

    private void requireExpectedOwnerRead(
        FormOwnerReference expectedOwner,
        FormActorContext actorContext
    ) {
        Long formId = expectedOwner == null ? null : expectedOwner.formId();
        requireRead(formId, expectedOwner, actorContext);
    }

    private static void requireRespondentResponse(
        FormResponse response,
        Long expectedRespondentMemberId,
        FormOwnerReference expectedOwner
    ) {
        requireSameRoot(response.getForm().getId(), expectedOwner);
        if (!expectedRespondentMemberId.equals(response.getRespondentMemberId())) {
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

    private static void requireSameRoot(
        Long resolvedFormId,
        FormOwnerReference expectedOwner
    ) {
        if (resolvedFormId == null
            || expectedOwner == null
            || !resolvedFormId.equals(expectedOwner.formId())) {
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
}
