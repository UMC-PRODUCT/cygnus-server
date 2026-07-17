package com.umc.product.form.application.service;

import org.springframework.stereotype.Service;

import com.umc.product.form.application.port.in.FormActorContext;
import com.umc.product.form.application.port.in.FormOwnerReferenceFactory;
import com.umc.product.form.application.port.out.FormOwnerPolicy;
import com.umc.product.form.application.port.out.LoadFormOwnershipPort;
import com.umc.product.form.application.port.out.SaveFormOwnershipPort;
import com.umc.product.form.domain.FormOperation;
import com.umc.product.form.domain.FormOwnerReference;
import com.umc.product.form.domain.FormOwnership;
import com.umc.product.form.domain.exception.FormDomainException;
import com.umc.product.form.domain.exception.FormErrorCode;

import lombok.RequiredArgsConstructor;

/** Form root ownership assertion과 namespace policy dispatch를 한 순서로 강제한다. */
@Service
@RequiredArgsConstructor
public class FormOwnershipAccessService {

    private final LoadFormOwnershipPort loadFormOwnershipPort;
    private final SaveFormOwnershipPort saveFormOwnershipPort;
    private final FormOwnerPolicyRegistry policyRegistry;

    public void requireMutation(
        Long resolvedFormId,
        FormOwnerReference expectedOwner,
        FormActorContext actorContext,
        FormOperation operation
    ) {
        requireAccess(resolvedFormId, expectedOwner, actorContext, operation, true);
    }

    public void requireRead(
        Long resolvedFormId,
        FormOwnerReference expectedOwner,
        FormActorContext actorContext,
        FormOperation operation
    ) {
        requireAccess(resolvedFormId, expectedOwner, actorContext, operation, false);
    }

    public FormOwnerReference registerNewForm(
        Long savedFormId,
        FormOwnerReferenceFactory ownerFactory,
        FormActorContext actorContext,
        FormOperation operation
    ) {
        if (savedFormId == null || ownerFactory == null || actorContext == null || operation == null) {
            throw denied();
        }
        FormOwnerReference expectedOwner = ownerFactory.create(savedFormId);
        if (expectedOwner == null || !savedFormId.equals(expectedOwner.formId())) {
            throw denied();
        }
        requireActor(operation, actorContext);
        requirePolicyAllows(expectedOwner, operation, actorContext);
        return saveFormOwnershipPort.save(expectedOwner);
    }

    private void requireAccess(
        Long resolvedFormId,
        FormOwnerReference expectedOwner,
        FormActorContext actorContext,
        FormOperation operation,
        boolean lock
    ) {
        if (resolvedFormId == null || expectedOwner == null || actorContext == null || operation == null) {
            throw denied();
        }

        FormOwnership ownership = (lock
            ? loadFormOwnershipPort.findByFormIdForUpdate(resolvedFormId)
            : loadFormOwnershipPort.findByFormId(resolvedFormId))
            .orElseThrow(FormOwnershipAccessService::denied);
        FormOwnerReference actualOwner = ownership.toReference();
        if (!actualOwner.sameBinding(expectedOwner)) {
            throw denied();
        }

        requireActor(operation, actorContext);
        requirePolicyAllows(actualOwner, operation, actorContext);
    }

    private void requirePolicyAllows(
        FormOwnerReference ownerReference,
        FormOperation operation,
        FormActorContext actorContext
    ) {
        FormOwnerPolicy policy = policyRegistry.requirePolicy(ownerReference.namespace());
        if (!policy.allows(ownerReference, operation, actorContext)) {
            throw denied();
        }
    }

    private static void requireActor(FormOperation operation, FormActorContext actorContext) {
        if (operation.requiresAuthenticatedMember() && actorContext.authenticatedMemberId().isEmpty()) {
            throw denied();
        }
    }

    private static FormDomainException denied() {
        return new FormDomainException(FormErrorCode.FORM_OWNERSHIP_FORBIDDEN);
    }
}
