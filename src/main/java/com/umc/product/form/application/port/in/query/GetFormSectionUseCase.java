package com.umc.product.form.application.port.in.query;

import java.util.List;
import java.util.Optional;

import com.umc.product.form.application.port.in.FormActorContext;
import com.umc.product.form.application.port.in.query.dto.FormSectionInfo;
import com.umc.product.form.domain.FormOwnerReference;

/**
 * FormSection 조회 UseCase.
 */
public interface GetFormSectionUseCase {

    /**
     * 섹션 ID로 단건 조회. 없으면 Optional.empty.
     * expected owner binding과 READ policy는 섹션 존재 여부와 무관하게 먼저 검증한다.
     */
    Optional<FormSectionInfo> findById(FormOwnerReference expectedOwner, FormActorContext actorContext, Long sectionId);

    /**
     * 섹션 ID로 단건 조회. 없으면 예외.
     */
    FormSectionInfo getById(FormOwnerReference expectedOwner, FormActorContext actorContext, Long sectionId);

    /**
     * 폼에 속한 모든 섹션을 orderNo 오름차순으로 조회.
     * 반환된 각 섹션의 Form root는 expected owner와 정확히 일치해야 한다.
     */
    List<FormSectionInfo> listByFormId(FormOwnerReference expectedOwner, FormActorContext actorContext);
}
