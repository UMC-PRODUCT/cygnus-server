package com.umc.product.form.application.service.query;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        return loadFormResponsePort.findAllDraftByRespondentMemberId(respondentMemberId).stream()
            .map(FormResponseInfo::from)
            .toList();
    }

    @Override
    public Optional<FormResponseInfo> findDraftByFormIdAndRespondentMemberId(Long formId, Long respondentMemberId) {
        return loadFormResponsePort.findDraftByFormIdAndRespondentMemberId(formId, respondentMemberId)
            .map(FormResponseInfo::from);
    }

    @Override
    public Optional<FormResponseInfo> findSubmittedByFormIdAndRespondentMemberId(Long formId, Long respondentMemberId) {
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
            .map(formResponse -> FormResponseWithAnswersInfo.from(
                formResponse,
                getAnswerUseCase.listByFormResponseId(formResponseId)
            ));
    }

    @Override
    public Map<Long, FormResponseWithAnswersInfo> findResponsesWithAnswers(Set<Long> formResponseIds) {
        if (formResponseIds == null || formResponseIds.isEmpty()) {
            return Map.of();
        }

        List<FormResponse> formResponses = loadFormResponsePort.listByIdsWithForm(formResponseIds);
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
