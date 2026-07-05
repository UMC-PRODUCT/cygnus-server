package com.umc.product.form.application.port.out;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.umc.product.form.domain.FormSection;

public interface LoadFormSectionPort {

    Optional<FormSection> findById(Long sectionId);

    List<FormSection> listByFormId(Long formId);

    List<FormSection> listByFormIds(Collection<Long> formIds);
}
