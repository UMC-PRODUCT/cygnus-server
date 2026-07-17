package com.umc.product.feedback.application.policy;

import org.springframework.stereotype.Component;

import com.umc.product.feedback.application.port.in.query.GetUserFeedbackTemplateOwnershipUseCase;
import com.umc.product.feedback.application.port.in.query.dto.FeedbackTemplateOwnershipInfo;
import com.umc.product.form.application.port.in.FormActorContext;
import com.umc.product.form.application.port.out.FormOwnerPolicy;
import com.umc.product.form.domain.FormOperation;
import com.umc.product.form.domain.FormOwnerReference;

import lombok.RequiredArgsConstructor;

/**
 * {@code feedback.template} namespace의 Form consumer policy.
 *
 * <p>정책은 Feedback의 public scalar query만 사용하고 Feedback entity/repository를 Form engine에
 * 노출하지 않는다. template 상태와 actor credential을 확인하지 못하면 항상 거부한다.</p>
 */
@Component
@RequiredArgsConstructor
public class FeedbackTemplateOwnerPolicy implements FormOwnerPolicy {

    private final GetUserFeedbackTemplateOwnershipUseCase ownershipQuery;
    private final FeedbackTemplateOwnerReferenceFactory ownerReferenceFactory;

    @Override
    public String namespace() {
        return FeedbackTemplateOwnerReferenceFactory.NAMESPACE;
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

        FeedbackTemplateOwnershipInfo template;
        try {
            template = ownershipQuery
                .findOwnershipById(parseTemplateId(ownerReference.ownerResourceKey()))
                .orElse(null);
        } catch (RuntimeException ignored) {
            return false;
        }
        if (template == null
            || template.templateId() == null
            || !ownerReference.ownerResourceKey().equals(template.templateId().toString())
            || !ownerReference.formId().equals(template.formId())
            || !template.active()) {
            return false;
        }

        return switch (operation) {
            case READ, RESPOND -> actorContext.authenticatedMemberId().isPresent();
            case MANAGE_STRUCTURE, PUBLISH, DELETE, READ_RESPONSES -> false;
        };
    }

    private boolean isCanonical(FormOwnerReference ownerReference) {
        if (ownerReference == null
            || !FeedbackTemplateOwnerReferenceFactory.NAMESPACE.equals(ownerReference.namespace())
            || !FeedbackTemplateOwnerReferenceFactory.SLOT.equals(ownerReference.slot())) {
            return false;
        }
        Long templateId = parseTemplateId(ownerReference.ownerResourceKey());
        if (templateId == null) {
            return false;
        }
        return ownerReferenceFactory.create(templateId, ownerReference.formId()).sameBinding(ownerReference);
    }

    private static Long parseTemplateId(String ownerResourceKey) {
        if (ownerResourceKey == null || ownerResourceKey.isBlank()) {
            return null;
        }
        try {
            long templateId = Long.parseLong(ownerResourceKey);
            return templateId > 0 ? templateId : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
