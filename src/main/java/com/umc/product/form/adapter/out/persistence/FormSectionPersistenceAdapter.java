package com.umc.product.form.adapter.out.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.umc.product.form.application.port.out.LoadFormSectionPort;
import com.umc.product.form.application.port.out.SaveFormSectionPort;
import com.umc.product.form.domain.FormSection;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class FormSectionPersistenceAdapter implements SaveFormSectionPort, LoadFormSectionPort {

    private final FormSectionJpaRepository formSectionJpaRepository;
    private final FormSectionQueryRepository formSectionQueryRepository;

    @Override
    public FormSection save(FormSection formSection) {
        return formSectionJpaRepository.save(formSection);
    }

    @Override
    public List<FormSection> saveAll(List<FormSection> sections) {
        return formSectionJpaRepository.saveAll(sections);
    }

    @Override
    public Optional<FormSection> findById(Long sectionId) {
        return formSectionJpaRepository.findById(sectionId);
    }

    @Override
    public List<FormSection> listByFormId(Long formId) {
        return formSectionQueryRepository.findAllByFormId(formId);
    }

    @Override
    public List<FormSection> listByFormIds(Collection<Long> formIds) {
        return formSectionQueryRepository.findAllByFormIdIn(formIds);
    }

    @Override
    public void deleteById(Long sectionId) {
        formSectionJpaRepository.deleteById(sectionId);
    }

    @Override
    public void deleteByFormId(Long formId) {
        formSectionJpaRepository.deleteByFormId(formId);
    }
}
