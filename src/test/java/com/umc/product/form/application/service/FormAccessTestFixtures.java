package com.umc.product.form.application.service;

import com.umc.product.form.application.port.in.FormActorContext;
import com.umc.product.form.application.port.in.FormOwnerReferenceFactory;
import com.umc.product.form.domain.FormOwnerReference;

public final class FormAccessTestFixtures {

    private FormAccessTestFixtures() {
    }

    public static FormOwnerReference owner(Long formId) {
        return FormOwnerReference.of(
            formId,
            "form.standalone",
            formId.toString(),
            "default"
        );
    }

    public static FormOwnerReferenceFactory ownerFactory() {
        return FormOwnerReferenceFactory.standalone();
    }

    public static FormActorContext actor(Long memberId) {
        return FormActorContext.authenticated(memberId);
    }

    public static FormActorContext anonymous() {
        return FormActorContext.anonymous();
    }

    public static FormActorContext responseActor(String rawKey) {
        return FormActorContext.responseCredential(rawKey);
    }
}
