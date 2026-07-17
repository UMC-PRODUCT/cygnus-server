package com.umc.product.form.application.service.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.audit.application.port.in.annotation.Audited;
import com.umc.product.audit.domain.AuditAction;
import com.umc.product.form.application.port.in.FormActorContext;
import com.umc.product.form.application.port.in.FormOwnerReferenceFactory;
import com.umc.product.form.application.port.in.command.ManageFormUseCase;
import com.umc.product.form.application.port.in.command.dto.CreateDraftFormCommand;
import com.umc.product.form.application.port.in.command.dto.DeleteFormCommand;
import com.umc.product.form.application.port.in.command.dto.PublishFormCommand;
import com.umc.product.form.application.port.in.command.dto.UpdateFormCommand;
import com.umc.product.form.application.port.out.LoadFormPort;
import com.umc.product.form.application.port.out.SaveAnswerPort;
import com.umc.product.form.application.port.out.SaveFormPort;
import com.umc.product.form.application.port.out.SaveFormResponsePort;
import com.umc.product.form.application.port.out.SaveFormSectionPort;
import com.umc.product.form.application.port.out.SaveQuestionOptionPort;
import com.umc.product.form.application.port.out.SaveQuestionPort;
import com.umc.product.form.application.service.FormAnswerAttachmentUsageService;
import com.umc.product.form.application.service.FormOwnershipAccessService;
import com.umc.product.form.domain.Form;
import com.umc.product.form.domain.FormOperation;
import com.umc.product.form.domain.FormOwnerReference;
import com.umc.product.form.domain.exception.FormDomainException;
import com.umc.product.form.domain.exception.FormErrorCode;
import com.umc.product.global.exception.constant.Domain;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class FormCommandService implements ManageFormUseCase {

    private final LoadFormPort loadFormPort;
    private final SaveFormPort saveFormPort;
    private final SaveFormSectionPort saveFormSectionPort;
    private final SaveQuestionPort saveQuestionPort;
    private final SaveQuestionOptionPort saveQuestionOptionPort;
    private final SaveFormResponsePort saveFormResponsePort;
    private final SaveAnswerPort saveAnswerPort;
    private final FormOwnershipAccessService ownershipAccessService;
    private final FormAnswerAttachmentUsageService attachmentUsageService;

    @Audited(
        domain = Domain.FORM,
        action = AuditAction.CREATE,
        targetType = "Form",
        targetId = "#result",
        description = "'설문 폼 초안을 생성했습니다.'"
    )
    @Override
    public Long createDraft(
        FormOwnerReferenceFactory ownerFactory,
        FormActorContext actorContext,
        CreateDraftFormCommand command
    ) {
        Long creatorMemberId = requireAuthenticatedMember(actorContext);
        Form form = Form.createDraft(
            command.title(),
            creatorMemberId,
            command.description(),
            command.allowDuplicateResponses()
        );

        Form saved = saveFormPort.save(form);
        ownershipAccessService.registerNewForm(
            saved.getId(), ownerFactory, actorContext, FormOperation.MANAGE_STRUCTURE
        );
        return saved.getId();
    }

    @Override
    public void updateForm(
        FormOwnerReference expectedOwner,
        FormActorContext actorContext,
        UpdateFormCommand command
    ) {
        ownershipAccessService.requireMutation(
            command.formId(), expectedOwner, actorContext, FormOperation.MANAGE_STRUCTURE
        );
        Form form = loadFormPort.findById(command.formId())
            .orElseThrow(() -> new FormDomainException(FormErrorCode.FORM_NOT_FOUND));

        form.update(
            command.title(),
            command.description(),
            command.isAnonymous(),
            command.allowDuplicateResponses(),
            Boolean.TRUE.equals(command.clearDescription())
        );
        saveFormPort.save(form);
    }

    @Audited(
        domain = Domain.FORM,
        action = AuditAction.PUBLISH,
        targetType = "Form",
        targetId = "#command.formId()",
        description = "'설문 폼을 게시했습니다.'"
    )
    @Override
    public void publishForm(
        FormOwnerReference expectedOwner,
        FormActorContext actorContext,
        PublishFormCommand command
    ) {
        ownershipAccessService.requireMutation(
            command.formId(), expectedOwner, actorContext, FormOperation.PUBLISH
        );
        Form form = loadFormPort.findById(command.formId())
            .orElseThrow(() -> new FormDomainException(FormErrorCode.FORM_NOT_FOUND));

        form.publish();
        saveFormPort.save(form);
    }

    @Override
    public void deleteForm(
        FormOwnerReference expectedOwner,
        FormActorContext actorContext,
        DeleteFormCommand command
    ) {
        Long formId = command.formId();
        ownershipAccessService.requireMutation(
            formId, expectedOwner, actorContext, FormOperation.DELETE
        );

        // 응답 트리 (자식부터)
        attachmentUsageService.detachByFormId(formId);
        saveAnswerPort.deleteByFormId(formId);
        saveFormResponsePort.deleteByFormId(formId);

        // 폼 구조 (자식부터)
        saveQuestionOptionPort.deleteByFormId(formId);
        saveQuestionPort.deleteByFormId(formId);
        saveFormSectionPort.deleteByFormId(formId);

        // 폼 본체
        saveFormPort.deleteById(formId);
    }

    private static Long requireAuthenticatedMember(FormActorContext actorContext) {
        if (actorContext == null) {
            throw new FormDomainException(FormErrorCode.FORM_OWNERSHIP_FORBIDDEN);
        }
        return actorContext.authenticatedMemberId()
            .orElseThrow(() -> new FormDomainException(FormErrorCode.FORM_OWNERSHIP_FORBIDDEN));
    }
}
