package com.umc.product.form.application.service.query;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.umc.product.form.domain.FormOwnerReference;
import com.umc.product.form.domain.exception.FormDomainException;
import com.umc.product.form.domain.exception.FormErrorCode;

/** Collection query가 허용하는 Form owner 집합을 form ID별로 유일하게 고정한다. */
final class FormOwnerScope {

    private static final FormOwnerScope EMPTY = new FormOwnerScope(Map.of());

    private final Map<Long, FormOwnerReference> ownersByFormId;

    private FormOwnerScope(Map<Long, FormOwnerReference> ownersByFormId) {
        this.ownersByFormId = ownersByFormId;
    }

    static FormOwnerScope from(Collection<FormOwnerReference> expectedOwners) {
        if (expectedOwners == null || expectedOwners.isEmpty()) {
            return EMPTY;
        }

        Map<Long, FormOwnerReference> indexed = new LinkedHashMap<>();
        for (FormOwnerReference owner : expectedOwners) {
            if (owner == null || indexed.putIfAbsent(owner.formId(), owner) != null) {
                throw denied();
            }
        }
        return new FormOwnerScope(Collections.unmodifiableMap(indexed));
    }

    boolean isEmpty() {
        return ownersByFormId.isEmpty();
    }

    void requireNonEmpty() {
        if (isEmpty()) {
            throw denied();
        }
    }

    boolean contains(Long formId) {
        return formId != null && ownersByFormId.containsKey(formId);
    }

    FormOwnerReference require(Long formId) {
        FormOwnerReference owner = ownersByFormId.get(formId);
        if (owner == null) {
            throw denied();
        }
        return owner;
    }

    List<Long> formIds() {
        return List.copyOf(ownersByFormId.keySet());
    }

    List<FormOwnerReference> owners() {
        return List.copyOf(ownersByFormId.values());
    }

    private static FormDomainException denied() {
        return new FormDomainException(FormErrorCode.FORM_OWNERSHIP_FORBIDDEN);
    }
}
