package com.umc.product.form.application.service.query;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.form.application.port.in.FormActorContext;
import com.umc.product.form.application.port.in.query.GetQuestionOptionUseCase;
import com.umc.product.form.application.port.in.query.dto.QuestionOptionInfo;
import com.umc.product.form.application.port.out.LoadQuestionOptionPort;
import com.umc.product.form.application.port.out.LoadQuestionPort;
import com.umc.product.form.application.service.FormOwnershipAccessService;
import com.umc.product.form.domain.FormOperation;
import com.umc.product.form.domain.FormOwnerReference;
import com.umc.product.form.domain.Question;
import com.umc.product.form.domain.QuestionOption;
import com.umc.product.form.domain.exception.FormDomainException;
import com.umc.product.form.domain.exception.FormErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class QuestionOptionQueryService implements GetQuestionOptionUseCase {

    private final LoadQuestionOptionPort loadQuestionOptionPort;
    private final LoadQuestionPort loadQuestionPort;
    private final FormOwnershipAccessService ownershipAccessService;

    @Override
    public Optional<QuestionOptionInfo> findById(
        FormOwnerReference expectedOwner,
        FormActorContext actorContext,
        Long optionId
    ) {
        requireExpectedOwnerRead(expectedOwner, actorContext);
        Optional<QuestionOption> option = loadQuestionOptionPort.findById(optionId);
        option.ifPresent(value -> requireSameRoot(rootFormId(value.getQuestion()), expectedOwner));
        return option
            .map(QuestionOptionInfo::from);
    }

    @Override
    public QuestionOptionInfo getById(
        FormOwnerReference expectedOwner,
        FormActorContext actorContext,
        Long optionId
    ) {
        QuestionOption option = loadQuestionOptionPort.findById(optionId)
            .orElseThrow(() -> new FormDomainException(FormErrorCode.QUESTION_OPTION_NOT_FOUND));
        requireRead(rootFormId(option.getQuestion()), expectedOwner, actorContext);
        return QuestionOptionInfo.from(option);
    }

    @Override
    public List<QuestionOptionInfo> listByQuestionId(
        FormOwnerReference expectedOwner,
        FormActorContext actorContext,
        Long questionId
    ) {
        Question question = loadQuestionPort.findById(questionId)
            .orElseThrow(() -> new FormDomainException(FormErrorCode.QUESTION_NOT_FOUND));
        requireRead(rootFormId(question), expectedOwner, actorContext);
        List<QuestionOption> options = loadQuestionOptionPort.listByQuestionId(questionId);
        options.forEach(option -> requireSameQuestion(option, questionId, expectedOwner));
        return options.stream()
            .map(QuestionOptionInfo::from)
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

    private static void requireSameQuestion(
        QuestionOption option,
        Long expectedQuestionId,
        FormOwnerReference expectedOwner
    ) {
        if (option.getQuestion().getId() == null
            || !option.getQuestion().getId().equals(expectedQuestionId)) {
            throw new FormDomainException(FormErrorCode.FORM_OWNERSHIP_FORBIDDEN);
        }
        requireSameRoot(rootFormId(option.getQuestion()), expectedOwner);
    }
}
