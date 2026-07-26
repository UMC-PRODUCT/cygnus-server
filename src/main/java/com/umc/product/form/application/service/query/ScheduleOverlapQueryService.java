package com.umc.product.form.application.service.query;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.form.application.port.in.query.GetScheduleOverlapUseCase;
import com.umc.product.form.application.port.in.query.dto.ScheduleOverlapSlotInfo;
import com.umc.product.form.application.port.out.LoadAnswerPort;
import com.umc.product.form.application.port.out.LoadFormResponsePort;
import com.umc.product.form.application.port.out.LoadQuestionPort;
import com.umc.product.form.domain.Answer;
import com.umc.product.form.domain.FormResponse;
import com.umc.product.form.domain.Question;
import com.umc.product.form.domain.enums.FormResponseStatus;
import com.umc.product.form.domain.enums.QuestionType;
import com.umc.product.form.domain.exception.FormDomainException;
import com.umc.product.form.domain.exception.FormErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ScheduleOverlapQueryService implements GetScheduleOverlapUseCase {

    private final LoadFormResponsePort loadFormResponsePort;
    private final LoadAnswerPort loadAnswerPort;
    private final LoadQuestionPort loadQuestionPort;

    @Override
    public List<ScheduleOverlapSlotInfo> getOverlap(Long formId, Long questionId, Set<Long> formResponseIds) {
        if (formResponseIds == null || formResponseIds.isEmpty()) {
            return List.of();
        }

        validateQuestion(formId, questionId);
        validateResponses(formId, formResponseIds);

        List<Answer> answers = loadAnswerPort.listByFormResponseIds(formResponseIds);

        Map<Instant, Set<Long>> slotToResponseIds = new HashMap<>();
        for (Answer answer : answers) {
            if (answer.getAnsweredAsType() != QuestionType.SCHEDULE) continue;
            if (!questionId.equals(answer.getQuestion().getId())) continue;
            Set<Instant> times = answer.getTimes();
            if (times == null || times.isEmpty()) continue;
            Long responseId = answer.getFormResponse().getId();
            for (Instant t : times) {
                slotToResponseIds.computeIfAbsent(t, k -> new HashSet<>()).add(responseId);
            }
        }

        return slotToResponseIds.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(e -> new ScheduleOverlapSlotInfo(e.getKey(), e.getValue()))
            .toList();
    }

    private void validateQuestion(Long formId, Long questionId) {
        Question question = loadQuestionPort.findById(questionId)
            .orElseThrow(() -> new FormDomainException(FormErrorCode.QUESTION_NOT_FOUND));
        if (question.getFormSection() == null
            || !question.getFormSection().getForm().getId().equals(formId)) {
            throw new FormDomainException(FormErrorCode.QUESTION_IS_NOT_OWNED_BY_FORM);
        }
        if (question.getType() != QuestionType.SCHEDULE) {
            throw new FormDomainException(FormErrorCode.QUESTION_TYPE_MISMATCH);
        }
    }

    private void validateResponses(Long formId, Set<Long> formResponseIds) {
        List<FormResponse> loaded = loadFormResponsePort.listByIdsWithForm(formResponseIds);
        Map<Long, FormResponse> byId = new HashMap<>();
        for (FormResponse fr : loaded) {
            byId.put(fr.getId(), fr);
        }
        for (Long id : formResponseIds) {
            FormResponse response = byId.get(id);
            if (response == null) {
                throw new FormDomainException(FormErrorCode.FORM_RESPONSE_NOT_FOUND);
            }
            if (!response.getForm().getId().equals(formId)) {
                throw new FormDomainException(FormErrorCode.FORM_RESPONSE_NOT_IN_FORM);
            }
            if (response.getStatus() != FormResponseStatus.SUBMITTED) {
                throw new FormDomainException(FormErrorCode.FORM_RESPONSE_NOT_SUBMITTED);
            }
        }
    }
}
