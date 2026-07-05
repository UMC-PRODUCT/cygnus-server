package com.umc.product.form.adapter.out.persistence;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.umc.product.form.application.port.out.LoadFormPort;
import com.umc.product.form.application.port.out.SaveFormPort;
import com.umc.product.form.domain.Form;
import com.umc.product.form.domain.exception.FormDomainException;
import com.umc.product.form.domain.exception.FormErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class FormPersistenceAdapter implements SaveFormPort, LoadFormPort {

    private final FormJpaRepository formJpaRepository;

    @Override
    public Form save(Form form) {
        return formJpaRepository.save(form);
    }

    @Override
    public Optional<Form> findById(Long formId) {
        return formJpaRepository.findById(formId);
    }

    @Override
    public List<Form> batchGetByIds(Collection<Long> formIds) {
        if (formIds == null || formIds.isEmpty()) {
            return List.of();
        }

        List<Long> uniqueIds = formIds.stream()
            .collect(java.util.stream.Collectors.collectingAndThen(
                java.util.stream.Collectors.toCollection(LinkedHashSet::new),
                List::copyOf
            ));
        if (uniqueIds.isEmpty()) {
            return List.of();
        }

        List<Form> forms = formJpaRepository.findAllById(uniqueIds);
        if (forms.size() != uniqueIds.size()) {
            throw new FormDomainException(FormErrorCode.FORM_NOT_FOUND);
        }
        return forms;
    }

    @Override
    public void deleteById(Long formId) {
        formJpaRepository.deleteById(formId);
    }
}
