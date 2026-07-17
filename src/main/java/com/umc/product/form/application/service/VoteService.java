package com.umc.product.form.application.service;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.audit.application.port.in.annotation.Audited;
import com.umc.product.audit.domain.AuditAction;
import com.umc.product.form.application.port.in.FormActorContext;
import com.umc.product.form.application.port.in.FormOwnerReferenceFactory;
import com.umc.product.form.application.port.in.command.ManageFormUseCase;
import com.umc.product.form.application.port.in.command.ManageVoteUseCase;
import com.umc.product.form.application.port.in.command.dto.CreateVoteCommand;
import com.umc.product.form.application.port.in.command.dto.DeleteFormCommand;
import com.umc.product.form.application.port.out.SaveFormPort;
import com.umc.product.form.application.port.out.SaveFormSectionPort;
import com.umc.product.form.application.port.out.SaveQuestionOptionPort;
import com.umc.product.form.application.port.out.SaveQuestionPort;
import com.umc.product.form.domain.Form;
import com.umc.product.form.domain.FormOperation;
import com.umc.product.form.domain.FormOwnerReference;
import com.umc.product.form.domain.FormSection;
import com.umc.product.form.domain.Question;
import com.umc.product.form.domain.QuestionOption;
import com.umc.product.form.domain.enums.QuestionType;
import com.umc.product.form.domain.exception.FormDomainException;
import com.umc.product.form.domain.exception.FormErrorCode;
import com.umc.product.global.exception.constant.Domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class VoteService implements ManageVoteUseCase {

    private final SaveFormPort saveFormPort;
    private final SaveFormSectionPort saveFormSectionPort;
    private final SaveQuestionPort saveQuestionPort;
    private final SaveQuestionOptionPort saveQuestionOptionPort;
    private final ManageFormUseCase manageFormUseCase;
    private final FormOwnershipAccessService ownershipAccessService;

    @Audited(
        domain = Domain.FORM,
        action = AuditAction.CREATE,
        targetType = "Vote",
        targetId = "#result",
        description = "'투표를 생성했습니다.'"
    )
    @Override
    public Long createVote(
        FormOwnerReferenceFactory ownerFactory,
        FormActorContext actorContext,
        CreateVoteCommand command
    ) {
        Long creatorMemberId = requireAuthenticatedMember(actorContext);
        QuestionType qType = command.allowMultipleChoice()
            ? QuestionType.CHECKBOX
            : QuestionType.RADIO;

        // 1. 순수한 Form 생성 (상태는 무조건 PUBLISHED)
        Form form = Form.createPublished(creatorMemberId, command.title(), command.isAnonymous());
        Form savedForm = saveFormPort.save(form);
        ownershipAccessService.registerNewForm(
            savedForm.getId(), ownerFactory, actorContext, FormOperation.PUBLISH
        );

        // 2. 단일 섹션 생성
        FormSection section = FormSection.create(
            savedForm,
            command.title(),
            null,
            1L
        );

        FormSection savedSection = saveFormSectionPort.save(section);

        // 3. 단일 질문 생성
        Question question = Question.create(
            command.title(),
            qType,
            true, // isRequired
            1L    // orderNo
        );
        question.assignTo(savedSection);
        Question savedQuestion = saveQuestionPort.save(question);

        // 4. 질문에 대한 선택지(옵션) 생성
        AtomicInteger order = new AtomicInteger(1);
        List<QuestionOption> options = command.options().stream()
            .map(optContent -> {
                QuestionOption opt = QuestionOption.create(optContent, order.getAndIncrement(), false);
                opt.assignTo(savedQuestion);
                return opt;
            })
            .collect(Collectors.toList());
        saveQuestionOptionPort.saveAll(options);

        // 조립된 Form의 ID 반환
        return savedForm.getId();
    }

    @Override
    public void deleteVote(FormOwnerReference expectedOwner, FormActorContext actorContext) {
        Long formId = expectedOwner == null ? null : expectedOwner.formId();
        manageFormUseCase.deleteForm(
            expectedOwner,
            actorContext,
            DeleteFormCommand.builder()
                .formId(formId)
                .build()
        );
    }

    private static Long requireAuthenticatedMember(FormActorContext actorContext) {
        if (actorContext == null) {
            throw new FormDomainException(FormErrorCode.FORM_OWNERSHIP_FORBIDDEN);
        }
        return actorContext.authenticatedMemberId()
            .orElseThrow(() -> new FormDomainException(FormErrorCode.FORM_OWNERSHIP_FORBIDDEN));
    }
}
