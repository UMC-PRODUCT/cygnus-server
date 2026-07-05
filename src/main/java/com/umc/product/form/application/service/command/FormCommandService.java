package com.umc.product.form.application.service.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.audit.application.port.in.annotation.Audited;
import com.umc.product.audit.domain.AuditAction;
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
import com.umc.product.form.domain.Form;
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

    @Audited(
        domain = Domain.FORM,
        action = AuditAction.CREATE,
        targetType = "Form",
        targetId = "#result",
        description = "'설문 폼 초안을 생성했습니다.'"
    )
    @Override
    public Long createDraft(CreateDraftFormCommand command) {
        Form form = Form.createDraft(
            command.title(),
            command.createdMemberId(),
            command.description(),
            command.allowDuplicateResponses()
        );

        return saveFormPort.save(form).getId();
    }

    @Override
    public void updateForm(UpdateFormCommand command) {
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
    public void publishForm(PublishFormCommand command) {
        Form form = loadFormPort.findById(command.formId())
            .orElseThrow(() -> new FormDomainException(FormErrorCode.FORM_NOT_FOUND));

        form.publish();
        saveFormPort.save(form);
    }

    @Override
    public void deleteForm(DeleteFormCommand command) {
        Long formId = command.formId();

        // 응답 트리 (자식부터)
        saveAnswerPort.deleteByFormId(formId);
        saveFormResponsePort.deleteByFormId(formId);

        // 폼 구조 (자식부터)
        saveQuestionOptionPort.deleteByFormId(formId);
        saveQuestionPort.deleteByFormId(formId);
        saveFormSectionPort.deleteByFormId(formId);

        // 폼 본체
        saveFormPort.deleteById(formId);
    }
}
