package com.umc.product.form.application.port.in.query;

import java.util.List;
import java.util.Optional;

import com.umc.product.form.application.port.in.query.dto.FormSectionInfo;

/**
 * FormSection 조회 UseCase.
 */
public interface GetFormSectionUseCase {

    /**
     * 섹션 ID로 단건 조회. 없으면 Optional.empty.
     */
    Optional<FormSectionInfo> findById(Long sectionId);

    /**
     * 섹션 ID로 단건 조회. 없으면 예외.
     */
    FormSectionInfo getById(Long sectionId);

    /**
     * 폼에 속한 모든 섹션을 orderNo 오름차순으로 조회.
     */
    List<FormSectionInfo> listByFormId(Long formId);
}
