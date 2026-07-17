package com.umc.product.form.application.service;

import org.springframework.stereotype.Component;

import com.umc.product.form.application.port.in.FormActorContext;
import com.umc.product.form.application.port.out.FormOwnerPolicy;
import com.umc.product.form.application.port.out.LoadFormPort;
import com.umc.product.form.domain.Form;
import com.umc.product.form.domain.FormOperation;
import com.umc.product.form.domain.FormOwnerReference;

import lombok.RequiredArgsConstructor;

/** Engine-native {@code form.standalone} namespace policy. */
@Component
@RequiredArgsConstructor
public class StandaloneFormOwnerPolicy implements FormOwnerPolicy {

    public static final String NAMESPACE = "form.standalone";
    public static final String SLOT = "default";

    private final LoadFormPort loadFormPort;

    @Override
    public String namespace() {
        return NAMESPACE;
    }

    @Override
    public boolean allows(
        FormOwnerReference ownerReference,
        FormOperation operation,
        FormActorContext actorContext
    ) {
        if (!isCanonical(ownerReference) || operation == null || actorContext == null) {
            return false;
        }
        Form form = loadFormPort.findById(ownerReference.formId()).orElse(null);
        if (form == null) {
            return false;
        }
        boolean creator = actorContext.authenticatedMemberId()
            .filter(form.getCreatedMemberId()::equals)
            .isPresent();
        return switch (operation) {
            case MANAGE_STRUCTURE, PUBLISH, DELETE, READ_RESPONSES -> creator;
            case READ -> creator || form.isPublished();
            case RESPOND -> form.isPublished();
        };
    }

    private static boolean isCanonical(FormOwnerReference ownerReference) {
        return ownerReference != null
            && NAMESPACE.equals(ownerReference.namespace())
            && ownerReference.formId().toString().equals(ownerReference.ownerResourceKey())
            && SLOT.equals(ownerReference.slot());
    }
}
