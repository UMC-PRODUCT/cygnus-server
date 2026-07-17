package com.umc.product.form.application.port.out;

import java.util.List;
import java.util.Optional;

import com.umc.product.form.domain.FormOwnerReference;
import com.umc.product.form.domain.FormOwnership;

/** Form ownership binding 조회 및 직렬화 anchor 접근 Port. */
public interface LoadFormOwnershipPort {

    Optional<FormOwnership> findByFormId(Long formId);

    Optional<FormOwnership> findByFormIdForUpdate(Long formId);

    Optional<FormOwnership> findByOwner(String namespace, String ownerResourceKey, String slot);

    List<String> listDistinctNamespaces();

    default Optional<FormOwnership> findForUpdate(Long formId) {
        return findByFormIdForUpdate(formId);
    }

    default List<String> findDistinctNamespaces() {
        return listDistinctNamespaces();
    }

    default Optional<FormOwnerReference> findReferenceByFormId(Long formId) {
        return findByFormId(formId).map(FormOwnership::toReference);
    }

    default Optional<FormOwnerReference> findReferenceByFormIdForUpdate(Long formId) {
        return findByFormIdForUpdate(formId).map(FormOwnership::toReference);
    }
}
