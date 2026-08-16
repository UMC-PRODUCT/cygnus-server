package com.umc.product.form.application.port.out;

import java.util.List;

import com.umc.product.form.domain.FormSection;

public interface SaveFormSectionPort {

    FormSection save(FormSection formSection);

    List<FormSection> saveAll(List<FormSection> sections);

    void deleteById(Long sectionId);

    /**
     * 특정 폼에 속한 모든 섹션 삭제 (deleteForm cascade 용)
     */
    void deleteByFormId(Long formId);
}
