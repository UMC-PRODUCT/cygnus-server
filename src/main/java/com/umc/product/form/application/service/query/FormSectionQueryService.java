package com.umc.product.form.application.service.query;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.form.application.port.in.query.GetFormSectionUseCase;
import com.umc.product.form.application.port.in.query.dto.FormSectionInfo;
import com.umc.product.form.application.port.out.LoadFormSectionPort;
import com.umc.product.form.domain.exception.FormDomainException;
import com.umc.product.form.domain.exception.FormErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FormSectionQueryService implements GetFormSectionUseCase {

    private final LoadFormSectionPort loadFormSectionPort;

    @Override
    public Optional<FormSectionInfo> findById(Long sectionId) {
        return loadFormSectionPort.findById(sectionId)
            .map(FormSectionInfo::from);
    }

    @Override
    public FormSectionInfo getById(Long sectionId) {
        return loadFormSectionPort.findById(sectionId)
            .map(FormSectionInfo::from)
            .orElseThrow(() -> new FormDomainException(FormErrorCode.FORM_NOT_FOUND));
    }

    @Override
    public List<FormSectionInfo> listByFormId(Long formId) {
        return loadFormSectionPort.listByFormId(formId).stream()
            .map(FormSectionInfo::from)
            .toList();
    }
}
