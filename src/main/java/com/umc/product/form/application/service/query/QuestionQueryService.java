package com.umc.product.form.application.service.query;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.form.application.port.in.FormActorContext;
import com.umc.product.form.application.port.in.query.GetQuestionUseCase;
import com.umc.product.form.application.port.in.query.dto.QuestionInfo;
import com.umc.product.form.application.port.out.LoadFormSectionPort;
import com.umc.product.form.application.port.out.LoadQuestionPort;
import com.umc.product.form.application.service.FormOwnershipAccessService;
import com.umc.product.form.domain.FormOperation;
import com.umc.product.form.domain.FormOwnerReference;
import com.umc.product.form.domain.FormSection;
import com.umc.product.form.domain.Question;
import com.umc.product.form.domain.exception.FormDomainException;
import com.umc.product.form.domain.exception.FormErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class QuestionQueryService implements GetQuestionUseCase {

    private final LoadQuestionPort loadQuestionPort;
    private final LoadFormSectionPort loadFormSectionPort;
    private final FormOwnershipAccessService ownershipAccessService;

    @Override
    public Optional<QuestionInfo> findById(
        FormOwnerReference expectedOwner,
        FormActorContext actorContext,
        Long questionId
    ) {
        requireExpectedOwnerRead(expectedOwner, actorContext);
        Optional<Question> question = loadQuestionPort.findById(questionId);
        question.ifPresent(value -> requireSameRoot(rootFormId(value), expectedOwner));
        return question
            .map(QuestionInfo::from);
    }

    @Override
    public QuestionInfo getById(
        FormOwnerReference expectedOwner,
        FormActorContext actorContext,
        Long questionId
    ) {
        Question question = loadQuestionPort.findById(questionId)
            .orElseThrow(() -> new FormDomainException(FormErrorCode.QUESTION_NOT_FOUND));
        requireRead(rootFormId(question), expectedOwner, actorContext);
        return QuestionInfo.from(question);
    }

    @Override
    public List<QuestionInfo> listBySectionId(
        FormOwnerReference expectedOwner,
        FormActorContext actorContext,
        Long sectionId
    ) {
        FormSection section = loadFormSectionPort.findById(sectionId)
            .orElseThrow(() -> new FormDomainException(FormErrorCode.FORM_NOT_FOUND));
        requireRead(section.getForm().getId(), expectedOwner, actorContext);
        List<Question> questions = loadQuestionPort.listBySectionId(sectionId);
        questions.forEach(question -> requireSameSection(question, sectionId, expectedOwner));
        return questions.stream()
            .map(QuestionInfo::from)
            .toList();
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

    private void requireExpectedOwnerRead(
        FormOwnerReference expectedOwner,
        FormActorContext actorContext
    ) {
        Long formId = expectedOwner == null ? null : expectedOwner.formId();
        requireRead(formId, expectedOwner, actorContext);
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

    private static Long rootFormId(Question question) {
        return question.getFormSection().getForm().getId();
    }

    private static void requireSameSection(
        Question question,
        Long expectedSectionId,
        FormOwnerReference expectedOwner
    ) {
        if (question.getFormSection().getId() == null
            || !question.getFormSection().getId().equals(expectedSectionId)) {
            throw new FormDomainException(FormErrorCode.FORM_OWNERSHIP_FORBIDDEN);
        }
        requireSameRoot(rootFormId(question), expectedOwner);
    }
}
