package com.umc.product.form.application.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.umc.product.form.application.port.out.FormOwnerPolicy;
import com.umc.product.form.domain.exception.FormDomainException;
import com.umc.product.form.domain.exception.FormErrorCode;

/** namespace마다 정확히 하나의 Form owner policy를 보관한다. */
@Component
public class FormOwnerPolicyRegistry {

    private final Map<String, FormOwnerPolicy> policiesByNamespace;

    public FormOwnerPolicyRegistry(List<FormOwnerPolicy> policies) {
        Map<String, FormOwnerPolicy> indexed = new LinkedHashMap<>();
        for (FormOwnerPolicy policy : policies) {
            if (policy == null || policy.namespace() == null || policy.namespace().isBlank()) {
                throw new IllegalStateException("Form owner policy namespace는 필수입니다.");
            }
            FormOwnerPolicy duplicate = indexed.putIfAbsent(policy.namespace(), policy);
            if (duplicate != null) {
                throw new IllegalStateException(
                    "Form owner policy evaluator가 중복되었습니다: " + policy.namespace()
                );
            }
        }
        this.policiesByNamespace = Map.copyOf(indexed);
    }

    public FormOwnerPolicy requirePolicy(String namespace) {
        FormOwnerPolicy policy = policiesByNamespace.get(namespace);
        if (policy == null) {
            throw new FormDomainException(FormErrorCode.FORM_OWNERSHIP_FORBIDDEN);
        }
        return policy;
    }

    public List<String> namespaces() {
        return policiesByNamespace.keySet().stream().sorted().toList();
    }
}
