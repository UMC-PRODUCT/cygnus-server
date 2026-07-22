package com.umc.product.form.application.service.query;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.authentication.application.service.SecureTokenGenerator;
import com.umc.product.form.application.port.in.query.GetAnswerUseCase;
import com.umc.product.form.application.port.in.query.GetFormResponseUseCase;
import com.umc.product.form.application.port.in.query.dto.AnswerInfo;
import com.umc.product.form.application.port.in.query.dto.FormResponseInfo;
import com.umc.product.form.application.port.in.query.dto.FormResponseWithAnswersInfo;
import com.umc.product.form.application.port.out.LoadFormResponsePort;
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
    // authentication 도메인의 공용 crypto util 재사용 (SSO Auth Code 발급과 동일 패턴).
    // 재배치(common/security 등) 는 별도 리팩터 PR 대상.
    private final SecureTokenGenerator secureTokenGenerator;

    @Override
    public boolean existsByFormId(Long formId) {
        return loadFormResponsePort.existsByFormId(formId);
    }

    @Override
    public Optional<FormResponseInfo> findById(Long formResponseId) {
        return loadFormResponsePort.findById(formResponseId)
            .map(FormResponseInfo::from);
    }

    @Override
    public FormResponseInfo getById(Long formResponseId) {
        return loadFormResponsePort.findById(formResponseId)
            .map(FormResponseInfo::from)
            .orElseThrow(() -> new FormDomainException(FormErrorCode.FORM_RESPONSE_NOT_FOUND));
    }

    @Override
    public List<FormResponseInfo> listByFormId(Long formId) {
        return loadFormResponsePort.listByFormId(formId).stream()
            .map(FormResponseInfo::from)
            .toList();
    }

    @Override
    public List<FormResponseInfo> listSubmittedByFormId(Long formId) {
        return loadFormResponsePort.listSubmittedByFormId(formId).stream()
            .map(FormResponseInfo::from)
            .toList();
    }

    @Override
    public List<FormResponseInfo> listDraftByRespondentMemberId(Long respondentMemberId) {
        requireRespondentMemberId(respondentMemberId);
        return loadFormResponsePort.findAllDraftByRespondentMemberId(respondentMemberId).stream()
            .map(FormResponseInfo::from)
            .toList();
    }

    @Override
    public Optional<FormResponseInfo> findDraftByFormIdAndRespondentMemberId(Long formId, Long respondentMemberId) {
        requireRespondentMemberId(respondentMemberId);
        return loadFormResponsePort.findDraftByFormIdAndRespondentMemberId(formId, respondentMemberId)
            .map(FormResponseInfo::from);
    }

    @Override
    public Optional<FormResponseInfo> findSubmittedByFormIdAndRespondentMemberId(Long formId, Long respondentMemberId) {
        requireRespondentMemberId(respondentMemberId);
        return loadFormResponsePort.findSubmittedByFormIdAndRespondentMemberId(formId, respondentMemberId)
            .map(FormResponseInfo::from);
    }

    @Override
    public FormResponseWithAnswersInfo getResponseWithAnswers(Long formResponseId) {
        return findResponseWithAnswers(formResponseId)
            .orElseThrow(() -> new FormDomainException(FormErrorCode.FORM_RESPONSE_NOT_FOUND));
    }

    @Override
    public Optional<FormResponseWithAnswersInfo> findResponseWithAnswers(Long formResponseId) {
        return loadFormResponsePort.findById(formResponseId)
            .filter(fr -> fr.getRespondentMemberId() != null)
            .map(formResponse -> FormResponseWithAnswersInfo.from(
                formResponse,
                getAnswerUseCase.listByFormResponseId(formResponseId)
            ));
    }

    private static void requireRespondentMemberId(Long respondentMemberId) {
        if (respondentMemberId == null) {
            throw new FormDomainException(FormErrorCode.RESPONDENT_MEMBER_ID_REQUIRED);
        }
    }

    @Override
    public Optional<FormResponseInfo> findByAccessKey(String rawKey) {
        return loadAnonymousResponseByAccessKey(rawKey)
            .map(FormResponseInfo::from);
    }

    @Override
    public FormResponseWithAnswersInfo getResponseWithAnswersByAccessKey(String rawKey) {
        FormResponse response = loadAnonymousResponseByAccessKey(rawKey)
            .orElseThrow(() -> new FormDomainException(FormErrorCode.FORM_RESPONSE_NOT_FOUND));
        return FormResponseWithAnswersInfo.from(
            response,
            getAnswerUseCase.listByFormResponseIdAsAnonymous(response.getId(), rawKey)
        );
    }

    /**
     * 익명 응답 조회 헬퍼 — rawKey 를 sha256 뜨고 hash 매칭. 기명 응답이 조회되면 방어 목적으로 empty.
     * <p>
     * rawKey 가 null 이면 {@link FormErrorCode#RESPONSE_ACCESS_KEY_REQUIRED}.
     */
    private Optional<FormResponse> loadAnonymousResponseByAccessKey(String rawKey) {
        if (rawKey == null) {
            throw new FormDomainException(FormErrorCode.RESPONSE_ACCESS_KEY_REQUIRED);
        }
        String hash = secureTokenGenerator.sha256Hex(rawKey);
        return loadFormResponsePort.findByAccessKeyHash(hash)
            .filter(fr -> fr.getRespondentMemberId() == null);
    }

    @Override
    public Map<Long, FormResponseWithAnswersInfo> findResponsesWithAnswers(Set<Long> formResponseIds) {
        if (formResponseIds == null || formResponseIds.isEmpty()) {
            return Map.of();
        }

        List<FormResponse> formResponses = loadFormResponsePort.listByIdsWithForm(formResponseIds).stream()
            .filter(fr -> fr.getRespondentMemberId() != null)
            .toList();
        Map<Long, List<AnswerInfo>> answersByFormResponseId =
            getAnswerUseCase.listByFormResponseIds(formResponseIds);

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

    @Override
    public Map<Long, FormResponseWithAnswersInfo> findAnonymousResponsesWithAnswers(
        Map<Long, String> accessKeysByFormResponseId
    ) {
        if (accessKeysByFormResponseId == null || accessKeysByFormResponseId.isEmpty()) {
            return Map.of();
        }

        Set<Long> formResponseIds = accessKeysByFormResponseId.keySet();
        List<FormResponse> formResponses = loadFormResponsePort.listByIdsWithForm(formResponseIds).stream()
            .filter(formResponse -> formResponse.getRespondentMemberId() == null)
            .filter(formResponse -> {
                String rawAccessKey = accessKeysByFormResponseId.get(formResponse.getId());
                return rawAccessKey != null
                    && secureTokenGenerator.sha256Hex(rawAccessKey).equals(formResponse.getResponseAccessKeyHash());
            })
            .toList();
        Map<Long, List<AnswerInfo>> answersByFormResponseId =
            getAnswerUseCase.listByFormResponseIds(formResponseIds);

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
}
