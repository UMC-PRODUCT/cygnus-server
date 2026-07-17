package com.umc.product.form.application.service.query;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.form.application.port.in.FormActorContext;
import com.umc.product.form.application.port.in.query.GetFormSectionUseCase;
import com.umc.product.form.application.port.in.query.dto.FormSectionInfo;
import com.umc.product.form.application.port.out.LoadFormSectionPort;
import com.umc.product.form.application.service.FormOwnershipAccessService;
import com.umc.product.form.domain.FormOperation;
import com.umc.product.form.domain.FormOwnerReference;
import com.umc.product.form.domain.FormSection;
import com.umc.product.form.domain.exception.FormDomainException;
import com.umc.product.form.domain.exception.FormErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FormSectionQueryService implements GetFormSectionUseCase {

    private final LoadFormSectionPort loadFormSectionPort;
    private final FormOwnershipAccessService ownershipAccessService;

    @Override
    public Optional<FormSectionInfo> findById(
        FormOwnerReference expectedOwner,
        FormActorContext actorContext,
        Long sectionId
    ) {
        requireExpectedOwnerRead(expectedOwner, actorContext);
        Optional<FormSection> section = loadFormSectionPort.findById(sectionId);
        section.ifPresent(value -> requireSameRoot(value.getForm().getId(), expectedOwner));
        return section
            .map(FormSectionInfo::from);
    }

    @Override
    public FormSectionInfo getById(
        FormOwnerReference expectedOwner,
        FormActorContext actorContext,
        Long sectionId
    ) {
        FormSection section = loadFormSectionPort.findById(sectionId)
            .orElseThrow(() -> new FormDomainException(FormErrorCode.FORM_NOT_FOUND));
        requireRead(section.getForm().getId(), expectedOwner, actorContext);
        return FormSectionInfo.from(section);
    }

    @Override
    public List<FormSectionInfo> listByFormId(
        FormOwnerReference expectedOwner,
        FormActorContext actorContext
    ) {
        Long formId = expectedOwner == null ? null : expectedOwner.formId();
        requireRead(formId, expectedOwner, actorContext);
        List<FormSection> sections = loadFormSectionPort.listByFormId(formId);
        sections.forEach(section -> requireSameRoot(section.getForm().getId(), expectedOwner));
        return sections.stream()
            .map(FormSectionInfo::from)
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
}
